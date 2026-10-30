/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.service;

import com.emberone.certification.service.TenantCertDefinitionService;
import com.emberone.drivinglicense.service.TenantDrivingLicenseDefinitionService;
import com.emberone.dutyfunction.service.TenantDutyFunctionDefinitionService;
import com.emberone.federalstate.FederalStateService;
import com.emberone.federalstate.FederalStateSettings;
import com.emberone.federalstate.baseline.FederalStateBaselines;
import com.emberone.federalstate.domain.FederalState;
import com.emberone.onboardingdocument.service.TenantOnboardingDocumentDefinitionService;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.service.OrganizationTypeDefinitionService;
import com.emberone.permissiongroup.standard.PermissionGroupStandardDomain;
import com.emberone.platform.i18n.LocalizationService;
import com.emberone.platform.i18n.TenantLocaleService;
import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.qualification.service.TenantQualificationDefinitionService;
import com.emberone.rank.service.TenantRankCatalogService;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.domain.StandardKind;
import com.emberone.standard.domain.StandardSource;
import com.emberone.standard.repository.StandardRepository;
import com.emberone.trainingcourse.service.TenantTrainingCourseTypeService;
import com.emberone.workflow.service.TenantWorkflowDefinitionService;
import com.emberone.workflow.standard.WorkflowDefinitionStandardDomain;
import com.emberone.workflow.standard.WorkflowTypeMappingStandardDomain;
import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Syncs tenant catalogs and the read-only SYSTEM org-setup standard from {@link
 * FederalStateSettings}. Runs in its own write transaction under a per-tenant advisory lock; holds
 * no JVM-local state. Called when a tenant is created, never on the request path.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantBaselineSyncService {

    private final EntityManager entityManager;
    private final StandardRepository standardRepository;
    private final OrgSetupBaselineTemplateService orgSetupBaselineTemplateService;
    private final FederalStateService federalStateService;
    private final TenantCertDefinitionService tenantCertDefinitionService;
    private final TenantTrainingCourseTypeService tenantTrainingCourseTypeService;
    private final TenantDrivingLicenseDefinitionService tenantDrivingLicenseDefinitionService;
    private final TenantQualificationDefinitionService tenantQualificationDefinitionService;
    private final TenantRankCatalogService tenantRankCatalogService;
    private final TenantDutyFunctionDefinitionService tenantDutyFunctionDefinitionService;
    private final TenantOnboardingDocumentDefinitionService
            tenantOnboardingDocumentDefinitionService;
    private final TenantWorkflowDefinitionService tenantWorkflowDefinitionService;
    private final OrganizationTypeDefinitionService organizationTypeDefinitionService;
    private final WorkflowTypeMappingStandardDomain workflowTypeMappingStandardDomain;
    private final WorkflowDefinitionStandardDomain workflowDefinitionStandardDomain;
    private final PermissionGroupStandardDomain permissionGroupStandardDomain;
    private final StandardMaterializationService standardMaterializationService;
    private final TenantLocaleService tenantLocaleService;
    private final LocalizationService localizationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncForActiveTenant() {
        Long tenantId = TenantScopeGuard.requireActiveTenantId();
        FederalState federalState = federalStateService.resolveTenantFederalState(tenantId);
        var locale = tenantLocaleService.resolveTenantLocale(tenantId);
        FederalStateBaselines baselines = federalStateService.settings(federalState).baselines();
        // Serializes concurrent syncs of the same tenant across instances until commit.
        entityManager
                .createNativeQuery(
                        "SELECT count(*) FROM (SELECT pg_advisory_xact_lock("
                                + "hashtextextended('tenant-baseline:' || :tenantId, 0))) AS l")
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        String federalStateName =
                localizationService.getMessage("federalState." + federalState.name(), locale);
        String name =
                localizationService.getInterpolatedMessage(
                        "standard.systemBaseline.name",
                        Map.of("federalState", federalStateName),
                        locale);
        String description =
                localizationService.getMessageOrNull("standard.systemBaseline.description", locale);
        syncTenantCatalog(tenantId, baselines);
        upsertSystemOrgStandard(tenantId, federalState, baselines, name, description);
        ensureWorkflowApprovalsAcrossStandards(baselines);
    }

    private void syncTenantCatalog(Long tenantId, FederalStateBaselines baselines) {
        var tenant = baselines.tenant();
        tenantCertDefinitionService.seedBaselineDefinitionsIfMissing(
                tenantId, tenant.certificationDefinitions());
        tenantTrainingCourseTypeService.seedBaselineTypesIfMissing(
                tenantId, tenant.trainingCourseTypes());
        tenantDrivingLicenseDefinitionService.seedBaselineDefinitionsIfMissing(
                tenantId, tenant.drivingLicenseDefinitions());
        tenantQualificationDefinitionService.seedBaselineDefinitionsIfMissing(
                tenantId, tenant.qualificationDefinitions());
        tenantRankCatalogService.seedBaselineIfMissing(tenantId, tenant.ranks());
        tenantDutyFunctionDefinitionService.seedBaselineIfMissing(tenantId, tenant.functions());
        tenantOnboardingDocumentDefinitionService.seedBaselineDefinitionsIfMissing(
                tenantId, tenant.onboardingDocumentDefinitions());
        tenantWorkflowDefinitionService.syncFromBaseline(tenantId, tenant.workflowDefinitions());
        organizationTypeDefinitionService.seedBaselineTypesIfMissing(
                tenantId, tenant.organizationTypeDefinitions());
    }

    private void upsertSystemOrgStandard(
            Long tenantId,
            FederalState federalState,
            FederalStateBaselines baselines,
            String name,
            String description) {
        var existing = standardRepository.findBySource(StandardSource.SYSTEM);
        boolean created = existing.isEmpty();
        Standard baseline =
                existing.orElseGet(
                        () ->
                                standardRepository.save(
                                        Standard.builder()
                                                .tenantId(tenantId)
                                                .kind(StandardKind.ORGANIZATION_SETUP)
                                                .source(StandardSource.SYSTEM)
                                                .name(name)
                                                .description(description)
                                                .build()));
        boolean nameChanged = !Objects.equals(baseline.getName(), name);
        boolean descriptionChanged = !Objects.equals(baseline.getDescription(), description);
        baseline.setName(name);
        baseline.setDescription(description);
        baseline = standardRepository.save(baseline);
        orgSetupBaselineTemplateService.syncTemplates(baseline, baselines, created);
        if (!created) {
            for (Organization organization :
                    standardMaterializationService.organizationsForStandard(baseline)) {
                permissionGroupStandardDomain.reassignForOrg(organization.getId(), baseline);
                workflowTypeMappingStandardDomain.reassignForOrg(organization.getId(), baseline);
            }
        }
        if (created) {
            log.info(
                    "Seeded read-only SYSTEM standard {} for federal state {}",
                    baseline.getId(),
                    federalState);
        } else if (nameChanged || descriptionChanged) {
            log.debug("Updated SYSTEM standard metadata for {}", baseline.getId());
        }
    }

    /**
     * Ensures every org standard has workflow approval templates for all baseline workflow types.
     * Missing entries are inserted disabled and materialized into orgs (opt-in pattern).
     */
    private void ensureWorkflowApprovalsAcrossStandards(FederalStateBaselines baselines) {
        var mappings = baselines.organizationSetup().workflowTypeMappings();
        var definitions = baselines.tenant().workflowDefinitions();
        for (Standard standard : standardRepository.findAllByOrderByNameAsc()) {
            int addedMappings =
                    workflowTypeMappingStandardDomain.ensureMissingFromBaseline(standard, mappings);
            int addedDefinitions =
                    workflowDefinitionStandardDomain.ensureMissingFromBaseline(
                            standard, definitions);
            if (addedMappings == 0 && addedDefinitions == 0) {
                continue;
            }
            for (Organization organization :
                    standardMaterializationService.organizationsForStandard(standard)) {
                workflowTypeMappingStandardDomain.materializeForOrg(organization.getId(), standard);
                workflowDefinitionStandardDomain.materializeForOrg(organization.getId(), standard);
            }
            log.info(
                    "Synced {} new workflow approval(s) and {} definition template(s) into standard {} and its organizations",
                    addedMappings,
                    addedDefinitions,
                    standard.getId());
        }
    }
}
