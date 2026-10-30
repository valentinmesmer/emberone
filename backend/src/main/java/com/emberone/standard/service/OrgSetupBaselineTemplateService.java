/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.service;

import com.emberone.certification.standard.CertificationStandardDomain;
import com.emberone.drivinglicense.standard.DrivingLicenseStandardDomain;
import com.emberone.dutyfunction.standard.DutyFunctionStandardDomain;
import com.emberone.dutyfunction.standard.FunctionSelectionStandardDomain;
import com.emberone.federalstate.baseline.*;
import com.emberone.onboarding.service.StandardOnboardingStepService;
import com.emberone.organizationgroup.standard.OrgGroupTemplate;
import com.emberone.organizationgroup.standard.OrgGroupTemplateRepository;
import com.emberone.organizationgroup.standard.OrganizationGroupStandardDomain;
import com.emberone.permissiongroup.standard.OrgGroupPermissionGroupTemplate;
import com.emberone.permissiongroup.standard.OrgGroupPermissionGroupTemplateRepository;
import com.emberone.permissiongroup.standard.PermissionGroupTemplate;
import com.emberone.permissiongroup.standard.PermissionGroupTemplateRepository;
import com.emberone.platform.i18n.PermissionGroupNamingService;
import com.emberone.qualification.standard.QualificationStandardDomain;
import com.emberone.rank.standard.RankStandardDomain;
import com.emberone.standard.domain.Standard;
import com.emberone.trainingcourse.standard.TrainingCourseStandardDomain;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.standard.WorkflowDefinitionTemplate;
import com.emberone.workflow.standard.WorkflowDefinitionTemplateRepository;
import com.emberone.workflow.standard.WorkflowTypeMappingTemplate;
import com.emberone.workflow.standard.WorkflowTypeMappingTemplateRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Upserts org-setup standard templates from a federal-state baseline. */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgSetupBaselineTemplateService {

    private final OrgGroupTemplateRepository orgGroupTemplateRepository;
    private final PermissionGroupTemplateRepository permissionGroupTemplateRepository;
    private final OrgGroupPermissionGroupTemplateRepository groupMappingTemplateRepository;
    private final WorkflowTypeMappingTemplateRepository workflowTypeMappingTemplateRepository;
    private final WorkflowDefinitionTemplateRepository workflowDefinitionTemplateRepository;
    private final CertificationStandardDomain certificationStandardDomain;
    private final TrainingCourseStandardDomain trainingCourseStandardDomain;
    private final DrivingLicenseStandardDomain drivingLicenseStandardDomain;
    private final QualificationStandardDomain qualificationStandardDomain;
    private final RankStandardDomain rankStandardDomain;
    private final DutyFunctionStandardDomain dutyFunctionStandardDomain;
    private final FunctionSelectionStandardDomain functionSelectionStandardDomain;
    private final StandardOnboardingStepService standardOnboardingStepService;
    private final OrganizationGroupStandardDomain organizationGroupStandardDomain;
    private final PermissionGroupNamingService permissionGroupNamingService;

    /**
     * @param enableNewWorkflowMappings when {@code true}, new workflow mappings on non-system
     *     standards are enabled from the baseline; the Bavaria SYSTEM standard always follows the
     *     baseline enabled flag
     */
    @Transactional
    public void syncTemplates(
            Standard standard, FederalStateBaselines baselines, boolean enableNewWorkflowMappings) {
        if (!standard.isSystem()) {
            throw new IllegalArgumentException(
                    "Only SYSTEM standards can be synced from the org-setup baseline");
        }
        var organizationSetup = baselines.organizationSetup();
        var tenant = baselines.tenant();
        Long tenantId = standard.getTenantId();
        Long standardId = standard.getId();

        Set<String> orgGroupKeys = new HashSet<>();
        for (OrgGroupBaseline item : organizationSetup.organizationGroups()) {
            orgGroupKeys.add(item.configKey());
            OrgGroupTemplate template =
                    orgGroupTemplateRepository
                            .findByStandardIdAndConfigKey(standardId, item.configKey())
                            .orElseGet(
                                    () ->
                                            OrgGroupTemplate.builder()
                                                    .tenantId(tenantId)
                                                    .standardId(standardId)
                                                    .configKey(item.configKey())
                                                    .build());
            template.setName(item.name());
            template.setDescription(item.description());
            template.setGroupType(item.groupType());
            orgGroupTemplateRepository.save(template);
        }
        orgGroupTemplateRepository.findByStandardId(standardId).stream()
                .filter(template -> !orgGroupKeys.contains(template.getConfigKey()))
                .forEach(orgGroupTemplateRepository::delete);

        Set<String> permissionGroupKeys = new HashSet<>();
        for (PermissionGroupBaseline item : organizationSetup.permissionGroups()) {
            permissionGroupKeys.add(item.configKey());
            PermissionGroupTemplate template =
                    permissionGroupTemplateRepository
                            .findByStandardIdAndConfigKey(standardId, item.configKey())
                            .orElseGet(
                                    () ->
                                            PermissionGroupTemplate.builder()
                                                    .tenantId(tenantId)
                                                    .standardId(standardId)
                                                    .configKey(item.configKey())
                                                    .grants(new ArrayList<>())
                                                    .build());
            template.setName(
                    resolvePermissionGroupTemplateName(item.name(), item.groupType(), tenantId));
            template.setDescription(item.description());
            template.setGroupType(item.groupType());
            template.setPermissionTypes(
                    item.groupType().effectivePermissionTypes(item.permissionTypes()));
            permissionGroupTemplateRepository.save(template);
        }
        permissionGroupTemplateRepository.findByStandardId(standardId).stream()
                .filter(template -> !permissionGroupKeys.contains(template.getConfigKey()))
                .forEach(permissionGroupTemplateRepository::delete);

        Set<String> groupMappingKeys = new HashSet<>();
        for (GroupMappingBaseline item : organizationSetup.groupMappings()) {
            String mappingKey = item.orgGroupConfigKey() + "\0" + item.permissionGroupConfigKey();
            groupMappingKeys.add(mappingKey);
            OrgGroupPermissionGroupTemplate template =
                    groupMappingTemplateRepository
                            .findByStandardIdAndOrgGroupConfigKeyAndPermissionGroupConfigKey(
                                    standardId,
                                    item.orgGroupConfigKey(),
                                    item.permissionGroupConfigKey())
                            .orElseGet(
                                    () ->
                                            OrgGroupPermissionGroupTemplate.builder()
                                                    .tenantId(tenantId)
                                                    .standardId(standardId)
                                                    .orgGroupConfigKey(item.orgGroupConfigKey())
                                                    .permissionGroupConfigKey(
                                                            item.permissionGroupConfigKey())
                                                    .build());
            groupMappingTemplateRepository.save(template);
        }
        groupMappingTemplateRepository.findByStandardId(standardId).stream()
                .filter(
                        template ->
                                !groupMappingKeys.contains(
                                        template.getOrgGroupConfigKey()
                                                + "\0"
                                                + template.getPermissionGroupConfigKey()))
                .forEach(groupMappingTemplateRepository::delete);

        Set<String> workflowMappingKeys = new HashSet<>();
        for (WorkflowTypeMappingBaseline item : organizationSetup.workflowTypeMappings()) {
            workflowMappingKeys.add(item.configKey());
            Optional<WorkflowTypeMappingTemplate> existing =
                    workflowTypeMappingTemplateRepository.findByStandardIdAndConfigKey(
                            standardId, item.configKey());
            boolean isNew = existing.isEmpty();
            WorkflowTypeMappingTemplate template =
                    existing.orElseGet(
                            () ->
                                    WorkflowTypeMappingTemplate.builder()
                                            .tenantId(tenantId)
                                            .standardId(standardId)
                                            .configKey(item.configKey())
                                            .build());
            template.setWorkflowTypeId(item.workflowTypeId());
            if (isNew) {
                template.setOrgGroupConfigKey(item.orgGroupConfigKey());
                template.setAutoApprove(item.autoApprove());
                template.setSelfApproveAllowed(item.selfApproveAllowed());
                template.setEnabled(
                        standard.isSystem()
                                ? item.enabled()
                                : enableNewWorkflowMappings && item.enabled());
            } else if (standard.isSystem()) {
                template.setOrgGroupConfigKey(item.orgGroupConfigKey());
                template.setAutoApprove(item.autoApprove());
                template.setSelfApproveAllowed(item.selfApproveAllowed());
                template.setEnabled(item.enabled());
            }
            workflowTypeMappingTemplateRepository.save(template);
        }
        workflowTypeMappingTemplateRepository.findByStandardId(standardId).stream()
                .filter(
                        template ->
                                !workflowMappingKeys.contains(template.getConfigKey())
                                        || !WorkflowType.isKnownId(template.getWorkflowTypeId()))
                .forEach(workflowTypeMappingTemplateRepository::delete);

        Set<String> workflowDefinitionKeys = new HashSet<>();
        for (WorkflowDefinitionBaseline item : tenant.workflowDefinitions()) {
            workflowDefinitionKeys.add(item.configKey());
            Optional<WorkflowDefinitionTemplate> existing =
                    workflowDefinitionTemplateRepository.findByStandardIdAndConfigKey(
                            standardId, item.configKey());
            if (existing.isPresent()) {
                WorkflowDefinitionTemplate template = existing.get();
                template.setWorkflowTypeId(item.workflowTypeId());
                template.setStaticApproversExclusive(item.staticApproversExclusive());
                template.getStaticOrganizations().clear();
                workflowDefinitionTemplateRepository.save(template);
            } else {
                workflowDefinitionTemplateRepository.save(
                        WorkflowDefinitionTemplate.builder()
                                .tenantId(tenantId)
                                .standardId(standardId)
                                .configKey(item.configKey())
                                .workflowTypeId(item.workflowTypeId())
                                .staticApproversExclusive(item.staticApproversExclusive())
                                .staticOrganizations(new ArrayList<>())
                                .build());
            }
        }
        workflowDefinitionTemplateRepository.findByStandardId(standardId).stream()
                .filter(
                        template ->
                                !workflowDefinitionKeys.contains(template.getConfigKey())
                                        || !WorkflowType.isKnownId(template.getWorkflowTypeId()))
                .forEach(workflowDefinitionTemplateRepository::delete);

        certificationStandardDomain.replaceSelections(standard, tenant.certificationConfigKeys());
        trainingCourseStandardDomain.replaceSelections(
                standard, tenant.trainingCourseTypeConfigKeys());
        drivingLicenseStandardDomain.replaceSelections(
                standard, tenant.drivingLicenseDefinitionConfigKeys());
        qualificationStandardDomain.replaceSelections(standard, tenant.qualificationConfigKeys());
        rankStandardDomain.replaceSelections(standard, tenant.rankConfigKeys());
        functionSelectionStandardDomain.replaceSelections(standard, tenant.functionConfigKeys());
        dutyFunctionStandardDomain.replaceGrants(standard, tenant.functionGrants());
        standardOnboardingStepService.replaceEntryDocumentsStep(
                standard, tenant.onboardingDocumentConfigKeys());
        standardOnboardingStepService.ensureSystemOnboardingSteps(
                standard, tenant.systemOnboardingStepTypes());

        organizationGroupStandardDomain.ensureRequiredSystemGroups(standard);
        if (enableNewWorkflowMappings) {
            log.info("Synced org-setup baseline templates for standard {}", standardId);
        } else {
            log.debug("Synced org-setup baseline templates for standard {}", standardId);
        }
    }

    private String resolvePermissionGroupTemplateName(
            String name, com.emberone.permission.domain.PermissionGroupType type, Long tenantId) {
        if (name != null && !name.isBlank()) {
            return name;
        }
        if (type.isSystem()) {
            return permissionGroupNamingService.resolveLocalizedName(type, tenantId);
        }
        return name;
    }
}
