/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.service;

import com.emberone.admin.standard.dto.OrgGroupRemovalImpactDto;
import com.emberone.admin.standard.dto.StandardSwitchImpactDto;
import com.emberone.certification.standard.CertificationStandardDomain;
import com.emberone.drivinglicense.standard.DrivingLicenseStandardDomain;
import com.emberone.dutyfunction.standard.DutyFunctionStandardDomain;
import com.emberone.dutyfunction.standard.FunctionSelectionStandardDomain;
import com.emberone.module.domain.Module;
import com.emberone.onboarding.service.StandardOnboardingStepService;
import com.emberone.onboarding.service.StandardOnboardingStepService.StepReplacement;
import com.emberone.onboarding.standard.StandardOnboardingStep;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.domain.OrganizationTypeDefinition;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organization.service.OrganizationTypeDefinitionService;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.organizationgroup.standard.OrganizationGroupStandardDomain;
import com.emberone.permission.domain.PermissionGroupType;
import com.emberone.permission.domain.PermissionType;
import com.emberone.permissiongroup.standard.PermissionGroupStandardDomain;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.qualification.standard.QualificationStandardDomain;
import com.emberone.rank.standard.RankStandardDomain;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.domain.StandardAssignment;
import com.emberone.standard.domain.StandardModule;
import com.emberone.standard.domain.StandardSource;
import com.emberone.standard.event.OrganizationStandardOverrideChangedEvent;
import com.emberone.standard.event.OrganizationTypeStandardAssignedEvent;
import com.emberone.standard.event.StandardEnabledModulesChangedEvent;
import com.emberone.standard.repository.StandardAssignmentRepository;
import com.emberone.standard.repository.StandardModuleRepository;
import com.emberone.standard.repository.StandardRepository;
import com.emberone.standard.spi.StandardContentItem;
import com.emberone.standard.spi.StandardDomainRegistry;
import com.emberone.trainingcourse.standard.TrainingCourseStandardDomain;
import com.emberone.workflow.standard.WorkflowTypeMappingStandardDomain;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the standards library: list/get, create/clone, delete, and assignment per
 * organization type. Domain-specific template content is delegated to the registered {@link
 * com.emberone.standard.spi.StandardDomain} beans.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StandardService {

    private final StandardRepository standardRepository;
    private final StandardAssignmentRepository assignmentRepository;
    private final OrganizationRepository organizationRepository;
    private final StandardDomainRegistry registry;
    private final StandardMaterializationService materializationService;
    private final EffectiveStandardResolver effectiveStandardResolver;
    private final OrganizationGroupStandardDomain organizationGroupStandardDomain;
    private final PermissionGroupStandardDomain permissionGroupStandardDomain;
    private final WorkflowTypeMappingStandardDomain workflowTypeMappingStandardDomain;
    private final CertificationStandardDomain certificationStandardDomain;
    private final TrainingCourseStandardDomain trainingCourseStandardDomain;
    private final DrivingLicenseStandardDomain drivingLicenseStandardDomain;
    private final QualificationStandardDomain qualificationStandardDomain;
    private final RankStandardDomain rankStandardDomain;
    private final FunctionSelectionStandardDomain functionSelectionStandardDomain;
    private final DutyFunctionStandardDomain dutyFunctionStandardDomain;
    private final StandardOnboardingStepService standardOnboardingStepService;
    private final TenantBaselineSyncService tenantBaselineSyncService;
    private final OrganizationTypeDefinitionService organizationTypeDefinitionService;
    private final StandardModuleRepository standardModuleRepository;
    private final EnabledModuleResolver enabledModuleResolver;
    private final ApplicationEventPublisher eventPublisher;

    private static void requireCustom(Standard standard) {
        if (standard.isSystem()) {
            throw new ConflictException("System standards are read-only");
        }
    }

    private static void requireAssignable(Standard standard) {
        if (standard.isSystem()) {
            throw new ConflictException(
                    "System standards cannot be assigned; clone the template and assign your copy");
        }
    }

    private static Long requireTenantId() {
        return TenantScopeGuard.requireActiveTenantId();
    }

    @Transactional(readOnly = true)
    public List<Standard> listStandards() {
        return standardRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Standard getStandard(Long id) {
        Long tenantId = requireTenantId();
        Standard standard =
                standardRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Standard not found: " + id));
        TenantScopeGuard.requireEntityInActiveTenant(standard.getTenantId(), "Standard", id);
        return standard;
    }

    /**
     * Describe the full content (template items across all domains) of a standard for the admin UI.
     */
    @Transactional(readOnly = true)
    public List<StandardContentItem> describeContent(Long id) {
        return registry.describeContent(getStandard(id));
    }

    /**
     * @see TenantBaselineSyncService#syncForActiveTenant()
     */
    public void ensureTenantBaselineSynced() {
        tenantBaselineSyncService.syncForActiveTenant();
    }

    // --- Content editing (custom standards only)
    // ---------------------------------------------------
    // Each operation mutates the standard's templates via the owning domain and propagates the
    // change
    // onto the existing organizations of every org type the standard is assigned to.

    /**
     * Create a new editable custom standard by cloning an existing one ("Klonen &amp; anpassen").
     * Catalog selections (certifications, training courses, driving licenses, qualifications) are
     * set to every active tenant catalog item so the copy starts fully selected.
     */
    @Transactional
    public Standard cloneStandard(Long sourceId, String name, String description) {
        Standard source = getStandard(sourceId);
        Long tenantId = requireTenantId();
        if (standardRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A standard with name '" + name + "' already exists");
        }
        Standard target =
                standardRepository.save(
                        Standard.builder()
                                .tenantId(requireTenantId())
                                .kind(source.getKind())
                                .source(StandardSource.CUSTOM)
                                .name(name)
                                .description(description)
                                .baseStandardId(source.getId())
                                .build());
        registry.copyTemplates(source, target);
        copyModuleConfiguration(source, target);
        standardOnboardingStepService.copySteps(source, target);
        selectAllCatalogItems(target);
        organizationGroupStandardDomain.ensureRequiredSystemGroups(target);
        log.info("Cloned standard {} from {}", target.getId(), source.getId());
        return target;
    }

    @Transactional
    public Standard updateStandard(Long id, String name, String description) {
        Standard standard = getStandard(id);
        requireCustom(standard);
        if (!standard.getName().equalsIgnoreCase(name)
                && standardRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A standard with name '" + name + "' already exists");
        }
        standard.setName(name);
        standard.setDescription(description);
        Standard saved = standardRepository.save(standard);
        log.info("Updated standard {}", id);
        return saved;
    }

    @Transactional
    public void addOrgGroup(
            Long standardId, String name, String description, OrganizationGroupType type) {
        Standard standard = requireCustomStandard(standardId);
        organizationGroupStandardDomain.addTemplate(
                standard, name, description, type, organizationsFor(standard));
    }

    @Transactional
    public void updateOrgGroup(Long standardId, String configKey, String name, String description) {
        Standard standard = requireCustomStandard(standardId);
        organizationGroupStandardDomain.updateTemplate(
                standard, configKey, name, description, organizationsFor(standard));
    }

    @Transactional
    public void deleteOrgGroup(Long standardId, String configKey) {
        Standard standard = requireCustomStandard(standardId);
        // Safety: refuse to remove a group that a workflow still relies on as its approver, which
        // would
        // silently break that workflow's approval routing in every materialized organization.
        if (workflowTypeMappingStandardDomain.isOrgGroupUsedAsApprover(standard, configKey)) {
            throw new ConflictException(
                    "Organization group '"
                            + configKey
                            + "' is used as a workflow approver; "
                            + "reassign the affected workflow(s) before removing it");
        }
        List<Organization> orgs = organizationsFor(standard);
        permissionGroupStandardDomain.deleteMappingsForOrgGroup(standard, configKey);
        organizationGroupStandardDomain.deleteTemplate(standard, configKey, orgs);
    }

    @Transactional(readOnly = true)
    public OrgGroupRemovalImpactDto previewDeleteOrgGroup(Long standardId, String configKey) {
        Standard standard = requireCustomStandard(standardId);
        return organizationGroupStandardDomain.previewRemoval(
                standard, configKey, organizationsFor(standard));
    }

    @Transactional(readOnly = true)
    public StandardSwitchImpactDto previewAssignStandard(
            Long organizationTypeId, Long newStandardId) {
        Standard newStandard = getStandard(newStandardId);
        requireAssignable(newStandard);
        return materializationService.previewReassignForType(organizationTypeId, newStandard);
    }

    @Transactional(readOnly = true)
    public StandardSwitchImpactDto previewSetOrganizationStandard(
            Long organizationId, Long newStandardId) {
        Organization organization = requireOrganization(organizationId);
        Standard newStandard =
                newStandardId == null
                        ? effectiveStandardResolver
                                .resolveForOrganizationType(organization.getType().getId())
                                .orElseThrow(
                                        () ->
                                                new ResourceNotFoundException(
                                                        "No standard assigned for organization type "
                                                                + organization.getType().getId()))
                        : getStandard(newStandardId);
        return materializationService.previewReassignForOrg(organization, newStandard);
    }

    @Transactional
    public void addPermissionGroup(
            Long standardId,
            String name,
            String description,
            PermissionGroupType type,
            List<PermissionType> permissionTypes) {
        Standard standard = requireCustomStandard(standardId);
        requireModulesEnabled(standard, permissionTypes);
        permissionGroupStandardDomain.addTemplate(
                standard, name, description, type, permissionTypes, organizationsFor(standard));
    }

    @Transactional
    public void updatePermissionGroup(
            Long standardId,
            String configKey,
            String name,
            String description,
            List<PermissionType> permissionTypes) {
        Standard standard = requireCustomStandard(standardId);
        requireModulesEnabled(standard, permissionTypes);
        permissionGroupStandardDomain.updateTemplate(
                standard,
                configKey,
                name,
                description,
                permissionTypes,
                organizationsFor(standard));
    }

    /**
     * Enabled modules for a standard (always includes {@link Module#CORE}). Empty {@code
     * std_standard_modules} means every module is on.
     */
    @Transactional(readOnly = true)
    public Set<Module> getEnabledModules(Long standardId) {
        return enabledModuleResolver.forStandard(getStandard(standardId));
    }

    @Transactional
    public Set<Module> updateEnabledModules(Long standardId, Set<Module> modules) {
        Standard standard = requireCustomStandard(standardId);
        standardModuleRepository.deleteAllByStandardId(standard.getId());
        Set<Module> enabledToggleable =
                modules.stream().filter(Module::isToggleable).collect(Collectors.toSet());
        List<StandardModule> disabledRows =
                java.util.Arrays.stream(Module.values())
                        .filter(Module::isToggleable)
                        .filter(module -> !enabledToggleable.contains(module))
                        .map(
                                module ->
                                        StandardModule.builder()
                                                .tenantId(standard.getTenantId())
                                                .standardId(standard.getId())
                                                .module(module)
                                                .build())
                        .toList();
        standardModuleRepository.saveAll(disabledRows);
        Set<Module> enabled = Module.alwaysOnModules();
        enabled.addAll(enabledToggleable);
        eventPublisher.publishEvent(
                new StandardEnabledModulesChangedEvent(standard.getTenantId(), standard.getId()));
        permissionGroupStandardDomain.syncInstancesForEnabledModules(
                standard, enabled, organizationsFor(standard));
        return enabled;
    }

    private void copyModuleConfiguration(Standard source, Standard target) {
        List<StandardModule> copies =
                standardModuleRepository
                        .findAllByTenantIdAndStandardId(source.getTenantId(), source.getId())
                        .stream()
                        .filter(row -> row.getModule().isToggleable())
                        .map(
                                row ->
                                        StandardModule.builder()
                                                .tenantId(target.getTenantId())
                                                .standardId(target.getId())
                                                .module(row.getModule())
                                                .build())
                        .toList();
        standardModuleRepository.saveAll(copies);
    }

    private void requireModulesEnabled(Standard standard, List<PermissionType> permissionTypes) {
        Set<Module> enabledModules = getEnabledModules(standard.getId());
        List<PermissionType> disallowed =
                permissionTypes.stream()
                        .filter(
                                permissionType ->
                                        !enabledModules.contains(permissionType.getModule()))
                        .toList();
        if (!disallowed.isEmpty()) {
            throw new IllegalArgumentException(
                    "Permission(s) "
                            + disallowed
                            + " belong to a module that is not enabled for this standard");
        }
    }

    @Transactional
    public void deletePermissionGroup(Long standardId, String configKey) {
        Standard standard = requireCustomStandard(standardId);
        permissionGroupStandardDomain.deleteTemplate(
                standard, configKey, organizationsFor(standard));
    }

    @Transactional
    public void addGroupMapping(
            Long standardId, String orgGroupConfigKey, String permissionGroupConfigKey) {
        Standard standard = requireCustomStandard(standardId);
        permissionGroupStandardDomain.addMapping(
                standard, orgGroupConfigKey, permissionGroupConfigKey, organizationsFor(standard));
    }

    @Transactional
    public void deleteGroupMapping(
            Long standardId, String orgGroupConfigKey, String permissionGroupConfigKey) {
        Standard standard = requireCustomStandard(standardId);
        permissionGroupStandardDomain.deleteMapping(
                standard, orgGroupConfigKey, permissionGroupConfigKey, organizationsFor(standard));
    }

    @Transactional
    public void updateWorkflow(
            Long standardId,
            String configKey,
            String approverOrgGroupConfigKey,
            boolean autoApprove,
            boolean enabled,
            boolean selfApproveAllowed) {
        Standard standard = requireCustomStandard(standardId);
        organizationGroupStandardDomain.requireWorkflowApproverOrgGroup(
                standard, approverOrgGroupConfigKey);
        List<Organization> orgs = organizationsFor(standard);
        workflowTypeMappingStandardDomain.updateMappingTemplate(
                standard,
                configKey,
                approverOrgGroupConfigKey,
                autoApprove,
                enabled,
                selfApproveAllowed,
                orgs);
    }

    // --- Certifications (selection only; definitions live in tenant catalog) -------------------

    @Transactional
    public void updateCertificationSelections(Long standardId, List<String> definitionConfigKeys) {
        Standard standard = requireCustomStandard(standardId);
        certificationStandardDomain.replaceSelections(standard, definitionConfigKeys);
        for (Organization org : organizationsFor(standard)) {
            certificationStandardDomain.reassignForOrg(org.getId(), standard);
        }
    }

    // --- Training courses (selection only; types live in tenant catalog) -----------------------

    @Transactional
    public void updateTrainingCourseTypeSelections(
            Long standardId, List<String> definitionConfigKeys) {
        Standard standard = requireCustomStandard(standardId);
        trainingCourseStandardDomain.replaceSelections(standard, definitionConfigKeys);
        for (Organization org : organizationsFor(standard)) {
            trainingCourseStandardDomain.reassignForOrg(org.getId(), standard);
        }
    }

    @Transactional
    public void updateDrivingLicenseSelections(Long standardId, List<String> definitionConfigKeys) {
        Standard standard = requireCustomStandard(standardId);
        drivingLicenseStandardDomain.replaceSelections(standard, definitionConfigKeys);
        for (Organization org : organizationsFor(standard)) {
            drivingLicenseStandardDomain.reassignForOrg(org.getId(), standard);
        }
    }

    @Transactional
    public void updateQualificationSelections(Long standardId, List<String> definitionConfigKeys) {
        Standard standard = requireCustomStandard(standardId);
        qualificationStandardDomain.replaceSelections(standard, definitionConfigKeys);
        for (Organization org : organizationsFor(standard)) {
            qualificationStandardDomain.reassignForOrg(org.getId(), standard);
        }
    }

    @Transactional
    public void updateRankSelections(Long standardId, List<String> rankConfigKeys) {
        Standard standard = requireCustomStandard(standardId);
        rankStandardDomain.replaceSelections(standard, rankConfigKeys);
        for (Organization org : organizationsFor(standard)) {
            rankStandardDomain.reassignForOrg(org.getId(), standard);
        }
    }

    @Transactional
    public void updateFunctionSelections(Long standardId, List<String> functionConfigKeys) {
        Standard standard = requireCustomStandard(standardId);
        functionSelectionStandardDomain.replaceSelections(standard, functionConfigKeys);
        for (Organization org : organizationsFor(standard)) {
            functionSelectionStandardDomain.reassignForOrg(org.getId(), standard);
        }
    }

    @Transactional
    public void updateFunctionGrants(
            Long standardId, List<DutyFunctionStandardDomain.FunctionGrantReplacement> grants) {
        Standard standard = requireCustomStandard(standardId);
        dutyFunctionStandardDomain.replaceGrantsFromRequests(standard, grants);
        for (Organization org : organizationsFor(standard)) {
            dutyFunctionStandardDomain.reassignForOrg(org.getId(), standard);
        }
    }

    @Transactional
    public List<StandardOnboardingStep> listOnboardingSteps(Long standardId) {
        return standardOnboardingStepService.listSteps(getStandard(standardId));
    }

    @Transactional
    public List<StandardOnboardingStep> replaceOnboardingSteps(
            Long standardId, List<StepReplacement> replacements) {
        Standard standard = requireCustomStandard(standardId);
        return standardOnboardingStepService.replaceSteps(standard, replacements);
    }

    private void selectAllCatalogItems(Standard standard) {
        certificationStandardDomain.selectAllActiveDefinitions(standard);
        trainingCourseStandardDomain.selectAllActiveDefinitions(standard);
        drivingLicenseStandardDomain.selectAllActiveDefinitions(standard);
        qualificationStandardDomain.selectAllActiveDefinitions(standard);
        rankStandardDomain.selectAllActiveRanks(standard);
        functionSelectionStandardDomain.selectAllActiveFunctions(standard);
    }

    private Standard requireCustomStandard(Long standardId) {
        Standard standard = getStandard(standardId);
        requireCustom(standard);
        return standard;
    }

    /** Existing organizations of every org type the standard is currently assigned to. */
    private List<Organization> organizationsFor(Standard standard) {
        return materializationService.organizationsForStandard(standard);
    }

    @Transactional
    public void deleteStandard(Long id) {
        Standard standard = getStandard(id);
        requireCustom(standard);
        Long tenantId = requireTenantId();
        if (assignmentRepository.existsByStandardId(id)) {
            throw new ConflictException(
                    "Standard is assigned to one or more organization types; reassign before deleting");
        }
        if (organizationRepository.existsByStandardId(id)) {
            throw new ConflictException(
                    "Standard is set as an override on one or more organizations; reassign before deleting");
        }
        standardRepository.detachCloneLineage(id);
        standardRepository.delete(standard);
        eventPublisher.publishEvent(new StandardEnabledModulesChangedEvent(tenantId, id));
        log.info("Deleted custom standard {}", id);
    }

    // --- Per-organization standard override
    // --------------------------------------------------------

    /**
     * Organizations with an explicit per-org standard override (instead of inheriting their type
     * default).
     */
    @Transactional(readOnly = true)
    public List<Organization> organizationsWithStandardOverride() {
        return organizationRepository.findByStandardIdIsNotNull();
    }

    /**
     * Set ({@code standardId} non-null) or clear ({@code standardId} null, i.e. inherit the
     * org-type default) the per-org standard override for a single organization, then
     * re-materialize that organization against its new effective standard. Only custom standards
     * may be assigned.
     *
     * <p>Passes the target standard into rematerialization explicitly (like {@link
     * #assignStandard}): the effective org-standard cache still holds the previous value until
     * after commit.
     */
    @Transactional
    public Organization setOrganizationStandardOverride(Long organizationId, Long standardId) {
        Organization organization = requireOrganization(organizationId);
        Long tenantId = requireTenantId();
        Long previousStandardId = organization.getStandardId();
        Standard targetStandard;
        if (standardId == null) {
            organization.setStandardId(null);
            targetStandard =
                    effectiveStandardResolver
                            .resolveForOrganizationType(organization.getType().getId())
                            .orElse(null);
        } else {
            targetStandard = getStandard(standardId);
            requireAssignable(targetStandard);
            organization.setStandardId(targetStandard.getId());
        }
        Organization saved = organizationRepository.save(organization);
        if (targetStandard != null) {
            materializationService.reassignStandardForOrg(saved, targetStandard);
        } else {
            log.warn(
                    "No standard resolved for organization {} (type {}); skipping re-materialization",
                    organizationId,
                    organization.getType().getId());
        }
        eventPublisher.publishEvent(
                new OrganizationStandardOverrideChangedEvent(
                        tenantId, organizationId, previousStandardId, standardId));
        log.info("Set standard override {} for organization {}", standardId, organizationId);
        return saved;
    }

    private Organization requireOrganization(Long organizationId) {
        Organization organization =
                organizationRepository
                        .findById(organizationId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Organization not found: " + organizationId));
        TenantScopeGuard.requireEntityInActiveTenant(
                organization.getTenantId(), "Organization", organizationId);
        return organization;
    }

    @Transactional(readOnly = true)
    public List<StandardAssignment> listAssignments() {
        return assignmentRepository.findAll();
    }

    /**
     * Assign a standard to an organization type and re-materialize existing organizations of that
     * type.
     */
    @Transactional
    public StandardAssignment assignStandard(Long organizationTypeId, Long standardId) {
        Standard standard = getStandard(standardId);
        requireAssignable(standard);
        Long tenantId = requireTenantId();
        OrganizationTypeDefinition type =
                organizationTypeDefinitionService.requireActiveType(organizationTypeId);
        Long previousStandardId =
                assignmentRepository
                        .findByTenantIdAndOrganizationType_Id(tenantId, organizationTypeId)
                        .map(StandardAssignment::getStandardId)
                        .orElse(null);
        StandardAssignment assignment =
                assignmentRepository
                        .findByTenantIdAndOrganizationType_Id(tenantId, organizationTypeId)
                        .map(
                                existing -> {
                                    existing.setStandardId(standardId);
                                    return existing;
                                })
                        .orElseGet(
                                () ->
                                        StandardAssignment.builder()
                                                .tenantId(tenantId)
                                                .organizationType(type)
                                                .standardId(standardId)
                                                .build());
        StandardAssignment saved = assignmentRepository.save(assignment);
        materializationService.reassignStandardForType(organizationTypeId, standard);
        eventPublisher.publishEvent(
                new OrganizationTypeStandardAssignedEvent(
                        tenantId, organizationTypeId, previousStandardId, standardId));
        log.info("Assigned standard {} to organization type {}", standardId, organizationTypeId);
        return saved;
    }
}
