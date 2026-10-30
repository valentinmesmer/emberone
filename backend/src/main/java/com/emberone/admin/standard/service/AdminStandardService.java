/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.standard.service;

import com.emberone.admin.onboarding.dto.*;
import com.emberone.admin.standard.dto.*;
import com.emberone.module.domain.Module;
import com.emberone.onboarding.service.StandardOnboardingStepService;
import com.emberone.onboarding.spi.OnboardingStepHandlerRegistry;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.permission.domain.PermissionGroupType;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.domain.StandardAssignment;
import com.emberone.standard.repository.StandardAssignmentRepository;
import com.emberone.standard.service.EffectiveStandardResolver;
import com.emberone.standard.service.StandardService;
import com.emberone.standard.spi.StandardContentItem;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tenant-scoped admin facade over the standards module: library CRUD/clone, per-org-type
 * assignment, per-org overrides, and membership-impact previews. Sets the tenant context from the
 * path so RLS resolves to the right tenant.
 */
@Service
@RequiredArgsConstructor
public class AdminStandardService {

    private final StandardService standardService;
    private final EffectiveStandardResolver effectiveStandardResolver;
    private final OrganizationRepository organizationRepository;
    private final StandardAssignmentRepository assignmentRepository;
    private final OnboardingStepHandlerRegistry onboardingStepHandlerRegistry;

    private static List<PermissionType> parsePermissionTypes(List<String> names) {
        return names.stream().map(AdminStandardService::parsePermissionType).distinct().toList();
    }

    private static PermissionType parsePermissionType(String value) {
        try {
            return PermissionType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unknown permission type: " + value);
        }
    }

    private static OrganizationGroupType parseOrgGroupType(String value) {
        try {
            return OrganizationGroupType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unknown organization group type: " + value);
        }
    }

    private static PermissionGroupType parsePermissionGroupType(String value) {
        try {
            return PermissionGroupType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unknown permission group type: " + value);
        }
    }

    private static Module parseModule(String value) {
        try {
            return Module.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown module: " + value);
        }
    }

    private static StandardContentItemDto toContentItemDto(StandardContentItem item) {
        return StandardContentItemDto.builder()
                .domainKey(item.domainKey())
                .configKey(item.configKey())
                .title(item.title())
                .subtitle(item.subtitle())
                .permissionTypes(item.permissionTypes())
                .attributes(item.attributes())
                .editable(item.editable())
                .build();
    }

    private static StandardDto toDto(Standard standard) {
        return StandardDto.builder()
                .id(standard.getId())
                .kind(standard.getKind() != null ? standard.getKind().name() : null)
                .source(standard.getSource() != null ? standard.getSource().name() : null)
                .name(standard.getName())
                .description(standard.getDescription())
                .baseStandardId(standard.getBaseStandardId())
                .system(standard.isSystem())
                .build();
    }

    private static StandardModulesDto toModulesDto(Set<Module> modules) {
        return StandardModulesDto.builder()
                .modules(
                        modules.stream()
                                .filter(module -> module != Module.CORE)
                                .map(Enum::name)
                                .sorted()
                                .toList())
                .build();
    }

    @Transactional
    public List<StandardDto> listStandards(long tenantId) {
        return withTenantContext(
                tenantId,
                () ->
                        standardService.listStandards().stream()
                                .map(AdminStandardService::toDto)
                                .toList());
    }

    @Transactional
    public StandardDto cloneStandard(long tenantId, CreateStandardRequest request) {
        return withTenantContext(
                tenantId,
                () ->
                        toDto(
                                standardService.cloneStandard(
                                        request.getBaseStandardId(),
                                        request.getName(),
                                        request.getDescription())));
    }

    @Transactional
    public StandardDto updateStandard(long tenantId, Long id, UpdateStandardRequest request) {
        return withTenantContext(
                tenantId,
                () ->
                        toDto(
                                standardService.updateStandard(
                                        id, request.getName(), request.getDescription())));
    }

    @Transactional(readOnly = true)
    public StandardContentDto getContent(long tenantId, Long id) {
        return withTenantContext(tenantId, () -> buildContent(id));
    }

    @Transactional(readOnly = true)
    public StandardModulesDto getEnabledModules(long tenantId, Long id) {
        return withTenantContext(
                tenantId, () -> toModulesDto(standardService.getEnabledModules(id)));
    }

    @Transactional
    public StandardModulesDto updateEnabledModules(
            long tenantId, Long id, StandardModulesDto request) {
        return withTenantContext(
                tenantId,
                () -> {
                    Set<Module> modules =
                            request.getModules() == null
                                    ? Set.of()
                                    : request.getModules().stream()
                                            .map(AdminStandardService::parseModule)
                                            .collect(Collectors.toUnmodifiableSet());
                    return toModulesDto(standardService.updateEnabledModules(id, modules));
                });
    }

    @Transactional
    public StandardContentDto addOrgGroup(long tenantId, Long id, CreateOrgGroupRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    standardService.addOrgGroup(
                            id,
                            request.getName(),
                            request.getDescription(),
                            parseOrgGroupType(request.getType()));
                    return buildContent(id);
                });
    }

    @Transactional
    public StandardContentDto updateOrgGroup(
            long tenantId, Long id, String configKey, UpdateOrgGroupRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    standardService.updateOrgGroup(
                            id, configKey, request.getName(), request.getDescription());
                    return buildContent(id);
                });
    }

    @Transactional
    public StandardContentDto deleteOrgGroup(long tenantId, Long id, String configKey) {
        return withTenantContext(
                tenantId,
                () -> {
                    standardService.deleteOrgGroup(id, configKey);
                    return buildContent(id);
                });
    }

    @Transactional
    public StandardContentDto addPermissionGroup(
            long tenantId, Long id, CreatePermissionGroupRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    standardService.addPermissionGroup(
                            id,
                            request.getName(),
                            request.getDescription(),
                            parsePermissionGroupType(request.getType()),
                            parsePermissionTypes(request.getPermissionTypes()));
                    return buildContent(id);
                });
    }

    @Transactional
    public StandardContentDto updatePermissionGroup(
            long tenantId, Long id, String configKey, UpdatePermissionGroupRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    standardService.updatePermissionGroup(
                            id,
                            configKey,
                            request.getName(),
                            request.getDescription(),
                            parsePermissionTypes(request.getPermissionTypes()));
                    return buildContent(id);
                });
    }

    @Transactional
    public StandardContentDto deletePermissionGroup(long tenantId, Long id, String configKey) {
        return withTenantContext(
                tenantId,
                () -> {
                    standardService.deletePermissionGroup(id, configKey);
                    return buildContent(id);
                });
    }

    @Transactional
    public StandardContentDto addGroupMapping(
            long tenantId, Long id, CreateGroupMappingRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    standardService.addGroupMapping(
                            id,
                            request.getOrgGroupConfigKey(),
                            request.getPermissionGroupConfigKey());
                    return buildContent(id);
                });
    }

    @Transactional
    public StandardContentDto deleteGroupMapping(
            long tenantId, Long id, String orgGroupConfigKey, String permissionGroupConfigKey) {
        return withTenantContext(
                tenantId,
                () -> {
                    standardService.deleteGroupMapping(
                            id, orgGroupConfigKey, permissionGroupConfigKey);
                    return buildContent(id);
                });
    }

    @Transactional
    public StandardContentDto updateWorkflow(
            long tenantId, Long id, String configKey, UpdateWorkflowRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    standardService.updateWorkflow(
                            id,
                            configKey,
                            request.getApproverOrgGroupConfigKey(),
                            request.isAutoApprove(),
                            request.isEnabled(),
                            request.isSelfApproveAllowed());
                    return buildContent(id);
                });
    }

    // --- Certifications (selection only) ---------------------------------------------------------

    @Transactional
    public StandardContentDto updateCertificationSelections(
            long tenantId, Long id, UpdateCertificationSelectionsRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    standardService.updateCertificationSelections(
                            id, request.getDefinitionConfigKeys());
                    return buildContent(id);
                });
    }

    @Transactional
    public StandardContentDto updateTrainingCourseTypeSelections(
            long tenantId, Long id, UpdateTrainingCourseTypeSelectionsRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    standardService.updateTrainingCourseTypeSelections(
                            id, request.getDefinitionConfigKeys());
                    return buildContent(id);
                });
    }

    @Transactional
    public StandardContentDto updateDrivingLicenseSelections(
            long tenantId, Long id, UpdateDrivingLicenseSelectionsRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    standardService.updateDrivingLicenseSelections(
                            id, request.getDefinitionConfigKeys());
                    return buildContent(id);
                });
    }

    @Transactional
    public StandardContentDto updateQualificationSelections(
            long tenantId, Long id, UpdateQualificationSelectionsRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    standardService.updateQualificationSelections(
                            id, request.getDefinitionConfigKeys());
                    return buildContent(id);
                });
    }

    @Transactional
    public StandardContentDto updateRankSelections(
            long tenantId, Long id, UpdateRankSelectionsRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    standardService.updateRankSelections(id, request.getRankConfigKeys());
                    return buildContent(id);
                });
    }

    @Transactional
    public StandardContentDto updateFunctionSelections(
            long tenantId, Long id, UpdateFunctionSelectionsRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    standardService.updateFunctionSelections(id, request.getFunctionConfigKeys());
                    return buildContent(id);
                });
    }

    @Transactional
    public StandardContentDto updateFunctionGrants(
            long tenantId, Long id, UpdateFunctionGrantsRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    List<
                                    com.emberone.dutyfunction.standard.DutyFunctionStandardDomain
                                            .FunctionGrantReplacement>
                            replacements =
                                    request.getGrants().stream()
                                            .map(
                                                    grant ->
                                                            new com.emberone.dutyfunction.standard
                                                                    .DutyFunctionStandardDomain
                                                                    .FunctionGrantReplacement(
                                                                    grant
                                                                            .getOrganizationGroupConfigKey(),
                                                                    grant.getMembershipType(),
                                                                    grant.getFunctionConfigKey()))
                                            .toList();
                    standardService.updateFunctionGrants(id, replacements);
                    return buildContent(id);
                });
    }

    @Transactional(readOnly = true)
    public OnboardingStepTypesResponse listOnboardingStepTypes(long tenantId) {
        return withTenantContext(
                tenantId,
                () ->
                        OnboardingStepTypesResponse.builder()
                                .stepTypes(
                                        onboardingStepHandlerRegistry.availableTypes().stream()
                                                .sorted(Comparator.comparing(Enum::name))
                                                .map(
                                                        type ->
                                                                OnboardingStepTypeDto.builder()
                                                                        .stepType(type)
                                                                        .build())
                                                .toList())
                                .build());
    }

    @Transactional(readOnly = true)
    public OnboardingStepsResponse getOnboardingSteps(long tenantId, Long id) {
        return withTenantContext(
                tenantId,
                () -> OnboardingStepsResponse.builder().steps(toOnboardingStepDtos(id)).build());
    }

    @Transactional
    public OnboardingStepsResponse replaceOnboardingSteps(
            long tenantId, Long id, ReplaceOnboardingStepsRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    List<StandardOnboardingStepService.StepReplacement> replacements =
                            request.getSteps().stream()
                                    .map(
                                            step ->
                                                    new StandardOnboardingStepService
                                                            .StepReplacement(
                                                            step.getStepType(),
                                                            step.getSortOrder(),
                                                            step.getConfig()))
                                    .toList();
                    standardService.replaceOnboardingSteps(id, replacements);
                    return OnboardingStepsResponse.builder()
                            .steps(toOnboardingStepDtos(id))
                            .build();
                });
    }

    private List<OnboardingStepDto> toOnboardingStepDtos(Long standardId) {
        return standardService.listOnboardingSteps(standardId).stream()
                .map(
                        step ->
                                OnboardingStepDto.builder()
                                        .stepType(step.getStepType())
                                        .sortOrder(step.getSortOrder())
                                        .config(step.getConfig())
                                        .build())
                .toList();
    }

    /**
     * Build the full content DTO for a standard (must be called within an active tenant context).
     */
    private StandardContentDto buildContent(Long id) {
        List<StandardContentItemDto> items =
                standardService.describeContent(id).stream()
                        .map(AdminStandardService::toContentItemDto)
                        .toList();
        return StandardContentDto.builder()
                .standard(toDto(standardService.getStandard(id)))
                .items(items)
                .build();
    }

    @Transactional
    public void deleteStandard(long tenantId, Long id) {
        withTenantContext(
                tenantId,
                () -> {
                    standardService.deleteStandard(id);
                    return null;
                });
    }

    @Transactional(readOnly = true)
    public List<StandardAssignmentDto> listAssignments(long tenantId) {
        return withTenantContext(
                tenantId,
                () ->
                        assignmentRepository.findAll().stream()
                                .sorted(
                                        Comparator.comparing(
                                                        (StandardAssignment assignment) ->
                                                                assignment
                                                                        .getOrganizationType()
                                                                        .getName(),
                                                        String.CASE_INSENSITIVE_ORDER)
                                                .thenComparing(StandardAssignment::getId))
                                .map(this::toAssignmentDto)
                                .toList());
    }

    private StandardAssignmentDto toAssignmentDto(StandardAssignment assignment) {
        return StandardAssignmentDto.builder()
                .organizationTypeId(assignment.getOrganizationType().getId())
                .standardId(assignment.getStandardId())
                .build();
    }

    @Transactional
    public StandardAssignmentDto assignStandard(
            long tenantId, Long organizationTypeId, AssignStandardRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    var assignment =
                            standardService.assignStandard(
                                    organizationTypeId, request.getStandardId());
                    return StandardAssignmentDto.builder()
                            .organizationTypeId(assignment.getOrganizationType().getId())
                            .standardId(assignment.getStandardId())
                            .build();
                });
    }

    // --- Per-organization standard override
    // --------------------------------------------------------

    @Transactional(readOnly = true)
    public List<OrganizationStandardOverrideDto> listOrganizationOverrides(long tenantId) {
        return withTenantContext(
                tenantId,
                () -> {
                    Map<Long, String> standardNames =
                            standardService.listStandards().stream()
                                    .collect(Collectors.toMap(Standard::getId, Standard::getName));
                    return organizationRepository.findByStandardIdIsNotNull().stream()
                            .sorted(
                                    Comparator.comparing(
                                                    Organization::getName,
                                                    String.CASE_INSENSITIVE_ORDER)
                                            .thenComparing(Organization::getId))
                            .map(org -> toOverrideDto(org, standardNames))
                            .toList();
                });
    }

    private OrganizationStandardOverrideDto toOverrideDto(
            Organization org, Map<Long, String> standardNames) {
        return OrganizationStandardOverrideDto.builder()
                .organizationId(org.getId())
                .organizationName(org.getName())
                .organizationCode(org.getOrganizationCode())
                .organizationTypeId(org.getType().getId())
                .standardId(org.getStandardId())
                .standardName(standardNames.get(org.getStandardId()))
                .build();
    }

    @Transactional(readOnly = true)
    public OrganizationStandardDto getOrganizationStandard(long tenantId, Long organizationId) {
        return withTenantContext(tenantId, () -> buildOrganizationStandard(organizationId));
    }

    @Transactional
    public OrganizationStandardDto setOrganizationStandard(
            long tenantId, Long organizationId, SetOrganizationStandardRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    standardService.setOrganizationStandardOverride(
                            organizationId, request.getStandardId());
                    return buildOrganizationStandard(organizationId);
                });
    }

    /**
     * Assemble the standard resolution (override / type default / effective) for one organization.
     * Must run within an active tenant context.
     */
    private OrganizationStandardDto buildOrganizationStandard(Long organizationId) {
        Organization organization =
                organizationRepository
                        .findById(organizationId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Organization not found: " + organizationId));
        TenantScopeGuard.requireEntityInActiveTenant(
                organization.getTenantId(), "Organization", organizationId);
        Standard typeDefault =
                effectiveStandardResolver
                        .resolveForOrganizationType(organization.getType().getId())
                        .orElse(null);
        Standard override =
                organization.getStandardId() == null
                        ? null
                        : standardService.getStandard(organization.getStandardId());
        Standard effective = override != null ? override : typeDefault;
        return OrganizationStandardDto.builder()
                .organizationId(organization.getId())
                .organizationTypeId(organization.getType().getId())
                .overridden(override != null)
                .overrideStandardId(override == null ? null : override.getId())
                .typeDefaultStandardName(typeDefault == null ? null : typeDefault.getName())
                .effectiveStandardId(effective == null ? null : effective.getId())
                .effectiveStandardName(effective == null ? null : effective.getName())
                .build();
    }

    @Transactional(readOnly = true)
    public OrgGroupRemovalImpactDto getOrgGroupRemovalImpact(
            long tenantId, Long standardId, String configKey) {
        return withTenantContext(
                tenantId, () -> standardService.previewDeleteOrgGroup(standardId, configKey));
    }

    @Transactional(readOnly = true)
    public StandardSwitchImpactDto getAssignmentImpact(
            long tenantId, Long organizationTypeId, Long standardId) {
        return withTenantContext(
                tenantId,
                () -> standardService.previewAssignStandard(organizationTypeId, standardId));
    }

    @Transactional(readOnly = true)
    public StandardSwitchImpactDto getOrganizationStandardImpact(
            long tenantId, Long organizationId, Long standardId) {
        return withTenantContext(
                tenantId,
                () -> {
                    Organization organization =
                            organizationRepository
                                    .findById(organizationId)
                                    .orElseThrow(
                                            () ->
                                                    new ResourceNotFoundException(
                                                            "Organization not found: "
                                                                    + organizationId));
                    if (!organization.getTenantId().equals(tenantId)) {
                        throw new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Organization does not belong to this tenant");
                    }
                    return standardService.previewSetOrganizationStandard(
                            organizationId, standardId);
                });
    }

    private <T> T withTenantContext(long tenantId, Supplier<T> action) {
        TenantScopeGuard.bind(tenantId);
        TenantScopeGuard.requireActiveTenantMatches(tenantId);
        return action.get();
    }
}
