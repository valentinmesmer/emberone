/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.federalstate.baseline;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.permission.domain.PermissionGroupType;
import com.emberone.platform.i18n.SystemGroupNamingService;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Validates bundled federal-state baselines at startup. */
@Slf4j
@Component
@RequiredArgsConstructor
public class BaselineValidator {

    private final WorkflowHandlerRegistry workflowHandlerRegistry;
    private final SystemGroupNamingService systemGroupNamingService;

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public void validate(FederalStateBaselines baselines) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, OrganizationGroupType> orgGroupTypeByKey =
                baselines.organizationSetup().organizationGroups().stream()
                        .filter(item -> !isBlank(item.configKey()))
                        .collect(
                                Collectors.toMap(
                                        OrgGroupBaseline::configKey,
                                        OrgGroupBaseline::groupType,
                                        (a, b) -> a));
        validateTenant(baselines.tenant(), orgGroupTypeByKey, errors);
        validateOrganizationSetup(baselines.organizationSetup(), errors, warnings);
        if (!errors.isEmpty()) {
            throw new IllegalStateException(
                    "Invalid federal-state baseline: " + String.join("; ", errors));
        }
        for (String warning : warnings) {
            log.warn("Federal-state baseline: {}", warning);
        }
    }

    private void validateTenant(
            TenantBaseline baseline,
            Map<String, OrganizationGroupType> orgGroupTypeByKey,
            List<String> errors) {
        Set<String> certKeys =
                uniqueConfigKeys(
                        baseline.certificationDefinitions(),
                        CertificationDefinitionBaseline::configKey,
                        "certification",
                        errors);
        Set<String> trainingKeys =
                uniqueConfigKeys(
                        baseline.trainingCourseTypes(),
                        TrainingCourseTypeBaseline::configKey,
                        "trainingCourseType",
                        errors);
        Set<String> drivingLicenseKeys =
                uniqueConfigKeys(
                        baseline.drivingLicenseDefinitions(),
                        DrivingLicenseDefinitionBaseline::configKey,
                        "drivingLicenseDefinition",
                        errors);
        uniqueConfigKeys(
                baseline.qualificationDefinitions(),
                QualificationDefinitionBaseline::configKey,
                "qualification",
                errors);
        uniqueConfigKeys(
                baseline.onboardingDocumentDefinitions(),
                OnboardingDocumentDefinitionBaseline::configKey,
                "onboardingDocument",
                errors);
        Set<String> rankKeys =
                uniqueConfigKeys(baseline.ranks(), RankBaseline::configKey, "rank", errors);
        Set<String> functionKeys =
                uniqueConfigKeys(
                        baseline.functions(), FunctionBaseline::configKey, "function", errors);

        for (CertificationDefinitionBaseline item : baseline.certificationDefinitions()) {
            requireText(item.configKey(), item.name(), "certification", errors);
            if (item.intervalDays() != null && item.intervalDays() <= 0) {
                errors.add(
                        "certification '" + item.configKey() + "': intervalDays must be positive");
            }
            if (item.reminderDaysBeforeExpiry() != null && item.reminderDaysBeforeExpiry() <= 0) {
                errors.add(
                        "certification '"
                                + item.configKey()
                                + "': reminderDaysBeforeExpiry must be positive");
            }
        }
        for (TrainingCourseTypeBaseline item : baseline.trainingCourseTypes()) {
            requireText(item.configKey(), item.name(), "trainingCourseType", errors);
        }
        for (DrivingLicenseDefinitionBaseline item : baseline.drivingLicenseDefinitions()) {
            requireText(item.configKey(), item.name(), "drivingLicenseDefinition", errors);
        }
        for (OnboardingDocumentDefinitionBaseline item : baseline.onboardingDocumentDefinitions()) {
            requireText(item.configKey(), item.name(), "onboardingDocument", errors);
        }
        for (RankBaseline item : baseline.ranks()) {
            requireText(item.configKey(), item.name(), "rank", errors);
        }
        for (FunctionBaseline item : baseline.functions()) {
            requireText(item.configKey(), item.name(), "function", errors);
        }
        for (FunctionGrantBaseline item : baseline.functionGrants()) {
            if (isBlank(item.orgGroupConfigKey()) || isBlank(item.functionConfigKey())) {
                errors.add("functionGrant: org group and function config keys are required");
                continue;
            }
            if (item.membershipType() == null) {
                errors.add(
                        "functionGrant '"
                                + item.orgGroupConfigKey()
                                + "': membership type is required");
                continue;
            }
            if (!functionKeys.contains(item.functionConfigKey())) {
                errors.add("functionGrant: unknown function '" + item.functionConfigKey() + "'");
            }
            OrganizationGroupType orgGroupType = orgGroupTypeByKey.get(item.orgGroupConfigKey());
            if (orgGroupType == null) {
                errors.add("functionGrant: unknown org group '" + item.orgGroupConfigKey() + "'");
            } else if (!orgGroupType.supportsFunctionGrants()) {
                errors.add(
                        "functionGrant '"
                                + item.orgGroupConfigKey()
                                + "': not allowed for group type "
                                + orgGroupType);
            }
        }
        for (QualificationDefinitionBaseline item : baseline.qualificationDefinitions()) {
            requireText(item.configKey(), item.name(), "qualification", errors);
            if (item.trainingCourseConfigKeys().isEmpty()
                    && item.certDefinitionConfigKeys().isEmpty()
                    && item.drivingLicenseDefinitionConfigKeys().isEmpty()
                    && !item.requiresOrgDecision()) {
                errors.add(
                        "qualification '"
                                + item.configKey()
                                + "': requires at least one requirement or org decision");
            }
            for (String key : item.trainingCourseConfigKeys()) {
                if (!trainingKeys.contains(key)) {
                    errors.add(
                            "qualification '"
                                    + item.configKey()
                                    + "': unknown training course '"
                                    + key
                                    + "'");
                }
            }
            for (String key : item.certDefinitionConfigKeys()) {
                if (!certKeys.contains(key)) {
                    errors.add(
                            "qualification '"
                                    + item.configKey()
                                    + "': unknown certification '"
                                    + key
                                    + "'");
                }
            }
            for (String key : item.drivingLicenseDefinitionConfigKeys()) {
                if (!drivingLicenseKeys.contains(key)) {
                    errors.add(
                            "qualification '"
                                    + item.configKey()
                                    + "': unknown driving license '"
                                    + key
                                    + "'");
                }
            }
        }

        Set<Long> definitionTypes = new HashSet<>();
        for (WorkflowDefinitionBaseline item : baseline.workflowDefinitions()) {
            if (isBlank(item.configKey())) {
                errors.add("workflowDefinition: configKey is required");
                continue;
            }
            if (!definitionTypes.add(item.workflowTypeId())) {
                errors.add("workflowDefinition: duplicate workflowTypeId " + item.workflowTypeId());
            }
            if (!item.configKey().equals(String.valueOf(item.workflowTypeId()))) {
                errors.add(
                        "workflowDefinition '"
                                + item.configKey()
                                + "': configKey must match workflowTypeId");
            }
            if (!isKnownWorkflowType(item.workflowTypeId())) {
                errors.add("workflowDefinition '" + item.configKey() + "': unknown workflowTypeId");
            }
        }
        requireAllWorkflowTypesCovered(definitionTypes, "workflowDefinition", errors);

        Set<String> orgTypeKeys = new HashSet<>();
        for (TenantBaseline.OrganizationTypeDefinitionBaseline item :
                baseline.organizationTypeDefinitions()) {
            if (isBlank(item.configKey())) {
                errors.add("organizationType: configKey is required");
                continue;
            }
            if (!orgTypeKeys.add(item.configKey())) {
                errors.add("organizationType: duplicate configKey '" + item.configKey() + "'");
            }
            if (isBlank(item.name())) {
                errors.add("organizationType '" + item.configKey() + "': name is required");
            }
        }
    }

    private void validateOrganizationSetup(
            OrganizationSetupBaseline baseline, List<String> errors, List<String> warnings) {
        Set<String> orgGroupKeys = new HashSet<>();
        Set<OrganizationGroupType> systemOrgGroupTypes = new HashSet<>();
        boolean hasLeadership = false;
        boolean hasAllMembers = false;
        Set<String> displayNames = new HashSet<>();

        for (OrgGroupBaseline item : baseline.organizationGroups()) {
            if (isBlank(item.configKey())) {
                errors.add("organizationGroup: configKey is required");
                continue;
            }
            if (!orgGroupKeys.add(item.configKey())) {
                errors.add("organizationGroup: duplicate configKey '" + item.configKey() + "'");
            }
            OrganizationGroupType type = item.groupType();
            if (type.isSystem() && !systemOrgGroupTypes.add(type)) {
                errors.add("organizationGroup: duplicate system type '" + type + "'");
            }
            if (type == OrganizationGroupType.LEADERSHIP) {
                hasLeadership = true;
            }
            if (type == OrganizationGroupType.ALL_MEMBERS) {
                hasAllMembers = true;
            }
            if (type.isSystem() && !type.name().equals(item.configKey())) {
                errors.add(
                        "organizationGroup '"
                                + item.configKey()
                                + "': system configKey must match groupType");
            }
            if (type == OrganizationGroupType.CUSTOM && isBlank(item.name())) {
                errors.add(
                        "organizationGroup '"
                                + item.configKey()
                                + "': name is required for custom groups");
            }
            String displayName = resolveOrgGroupDisplayName(item);
            if (displayName != null
                    && !displayNames.add(displayName.trim().toLowerCase(Locale.ROOT))) {
                errors.add(
                        "organizationGroup: duplicate display name '" + displayName.trim() + "'");
            }
        }
        if (!hasLeadership) {
            warnings.add("Missing LEADERSHIP organization group; it will be added automatically");
        }
        if (!hasAllMembers) {
            warnings.add("Missing ALL_MEMBERS organization group; it will be added automatically");
        }

        Set<String> permissionGroupKeys = new HashSet<>();
        Set<PermissionGroupType> systemPermissionGroupTypes = new HashSet<>();
        for (PermissionGroupBaseline item : baseline.permissionGroups()) {
            if (isBlank(item.configKey())) {
                errors.add("permissionGroup: configKey is required");
                continue;
            }
            if (!permissionGroupKeys.add(item.configKey())) {
                errors.add("permissionGroup: duplicate configKey '" + item.configKey() + "'");
            }
            PermissionGroupType type = item.groupType();
            if (isBlank(item.name()) && !type.isSystem()) {
                errors.add("permissionGroup '" + item.configKey() + "': name is required");
            }
            if (type.isSystem() && !systemPermissionGroupTypes.add(type)) {
                errors.add("permissionGroup: duplicate system type '" + type + "'");
            }
            if (type.isSystem() && !type.name().equals(item.configKey())) {
                errors.add(
                        "permissionGroup '"
                                + item.configKey()
                                + "': system configKey must match groupType");
            }
        }

        Set<String> mappingKeys = new HashSet<>();
        for (GroupMappingBaseline item : baseline.groupMappings()) {
            if (isBlank(item.orgGroupConfigKey()) || isBlank(item.permissionGroupConfigKey())) {
                errors.add("groupMapping: both config keys are required");
                continue;
            }
            String key = item.orgGroupConfigKey() + "->" + item.permissionGroupConfigKey();
            if (!mappingKeys.add(key)) {
                errors.add("groupMapping: duplicate mapping");
            }
            if (!orgGroupKeys.contains(item.orgGroupConfigKey())) {
                errors.add("groupMapping: unknown org group '" + item.orgGroupConfigKey() + "'");
            }
            if (!permissionGroupKeys.contains(item.permissionGroupConfigKey())) {
                errors.add(
                        "groupMapping: unknown permission group '"
                                + item.permissionGroupConfigKey()
                                + "'");
            }
        }

        Map<String, OrganizationGroupType> orgGroupTypeByKey =
                baseline.organizationGroups().stream()
                        .filter(item -> !isBlank(item.configKey()))
                        .collect(
                                Collectors.toMap(
                                        OrgGroupBaseline::configKey,
                                        OrgGroupBaseline::groupType,
                                        (a, b) -> a));

        Set<Long> mappingTypes = new HashSet<>();
        Set<String> workflowMappingKeys = new HashSet<>();
        for (WorkflowTypeMappingBaseline item : baseline.workflowTypeMappings()) {
            if (isBlank(item.configKey())) {
                errors.add("workflowTypeMapping: configKey is required");
                continue;
            }
            if (!workflowMappingKeys.add(item.configKey())) {
                errors.add("workflowTypeMapping: duplicate configKey '" + item.configKey() + "'");
            }
            if (!mappingTypes.add(item.workflowTypeId())) {
                errors.add(
                        "workflowTypeMapping: duplicate workflowTypeId " + item.workflowTypeId());
            }
            if (!item.configKey().equals(String.valueOf(item.workflowTypeId()))) {
                errors.add(
                        "workflowTypeMapping '"
                                + item.configKey()
                                + "': configKey must match workflowTypeId");
            }
            if (!isKnownWorkflowType(item.workflowTypeId())) {
                errors.add(
                        "workflowTypeMapping '" + item.configKey() + "': unknown workflowTypeId");
            }
            if (isBlank(item.orgGroupConfigKey())) {
                errors.add(
                        "workflowTypeMapping '"
                                + item.configKey()
                                + "': orgGroupConfigKey is required");
            } else if (!orgGroupKeys.contains(item.orgGroupConfigKey())) {
                errors.add("workflowTypeMapping '" + item.configKey() + "': unknown org group");
            } else {
                OrganizationGroupType approverType =
                        orgGroupTypeByKey.get(item.orgGroupConfigKey());
                if (approverType != null && !approverType.canApproveWorkflows()) {
                    errors.add(
                            "workflowTypeMapping '"
                                    + item.configKey()
                                    + "': organization group type "
                                    + approverType
                                    + " cannot approve workflows");
                }
            }
        }
        requireAllWorkflowTypesCovered(mappingTypes, "workflowTypeMapping", errors);
    }

    private <T> Set<String> uniqueConfigKeys(
            List<T> items,
            java.util.function.Function<T, String> keyExtractor,
            String label,
            List<String> errors) {
        Set<String> keys = new HashSet<>();
        for (T item : items) {
            String key = keyExtractor.apply(item);
            if (isBlank(key)) {
                errors.add(label + ": configKey is required");
                continue;
            }
            if (!keys.add(key)) {
                errors.add(label + ": duplicate configKey '" + key + "'");
            }
        }
        return keys;
    }

    private void requireText(String configKey, String name, String label, List<String> errors) {
        if (isBlank(configKey)) {
            errors.add(label + ": configKey is required");
            return;
        }
        if (isBlank(name)) {
            errors.add(label + " '" + configKey + "': name is required");
        }
    }

    private void requireAllWorkflowTypesCovered(
            Set<Long> covered, String label, List<String> errors) {
        for (WorkflowHandler<?> handler : workflowHandlerRegistry.all()) {
            Long workflowTypeId = handler.workflowTypeId();
            if (!covered.contains(workflowTypeId)) {
                errors.add("Missing " + label + " for workflowTypeId " + workflowTypeId);
            }
        }
    }

    private String resolveOrgGroupDisplayName(OrgGroupBaseline item) {
        if (!isBlank(item.name())) {
            return item.name();
        }
        if (item.groupType().isSystem()) {
            return systemGroupNamingService.resolveLocalizedName(
                    item.groupType(), SupportedLocale.DE);
        }
        return null;
    }

    private boolean isKnownWorkflowType(long workflowTypeId) {
        try {
            return workflowHandlerRegistry.require(workflowTypeId) != null;
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
