/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.federalstate.bayern;

import com.emberone.federalstate.baseline.*;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.permission.domain.PermissionGroupType;
import com.emberone.permission.domain.PermissionType;
import com.emberone.standard.util.StandardConfigKey;
import com.emberone.workflow.domain.WorkflowType;
import java.util.Arrays;
import java.util.List;

/**
 * Bavaria org-setup standard baseline (organization groups, permission groups, workflow mappings).
 */
public final class BayernOrganizationSetupBaseline {

    public static final String ORG_GROUP_LEADERSHIP_TEAM_NAME_KEY =
            "organizationGroup.custom.leadershipTeam";
    public static final String ORG_GROUP_HR_NAME_KEY = "organizationGroup.custom.hr";
    public static final String PERM_GROUP_POWERUSER_NAME_KEY = "permissionGroup.custom.poweruser";

    private BayernOrganizationSetupBaseline() {}

    public static OrganizationSetupBaseline build(Labels labels) {
        return new OrganizationSetupBaseline(
                organizationGroups(labels),
                permissionGroups(labels),
                groupMappings(),
                workflowTypeMappings());
    }

    private static List<OrgGroupBaseline> organizationGroups(Labels labels) {
        return List.of(
                orgGroup(OrganizationGroupType.LEADERSHIP),
                orgGroup(OrganizationGroupType.ALL_MEMBERS),
                customOrgGroup(ORG_GROUP_HR_NAME_KEY, labels.orgGroupHr()),
                customOrgGroup(
                        ORG_GROUP_LEADERSHIP_TEAM_NAME_KEY, labels.orgGroupLeadershipTeam()));
    }

    private static OrgGroupBaseline orgGroup(OrganizationGroupType type) {
        return new OrgGroupBaseline(type.name(), null, null, type);
    }

    private static OrgGroupBaseline customOrgGroup(String nameKey, String name) {
        return new OrgGroupBaseline(configKey(nameKey), name, null, OrganizationGroupType.CUSTOM);
    }

    private static List<PermissionGroupBaseline> permissionGroups(Labels labels) {
        return List.of(
                permissionGroup(PermissionGroupType.ADMIN, assignablePermissionTypes()),
                permissionGroup(
                        PermissionGroupType.STANDARD,
                        permissionTypes(
                                PermissionType.SERVICE_EVENT_READ,
                                PermissionType.OPERATION_EVENT_READ,
                                PermissionType.APPOINTMENT_READ,
                                PermissionType.EVENT_ATTENDANCE_READ,
                                PermissionType.PROFILE_IMAGE_READ)),
                customPermissionGroup(
                        PERM_GROUP_POWERUSER_NAME_KEY,
                        labels.permissionGroupPoweruser(),
                        permissionTypes(
                                PermissionType.SERVICE_EVENT_READ,
                                PermissionType.SERVICE_EVENT_WRITE,
                                PermissionType.OPERATION_EVENT_READ,
                                PermissionType.OPERATION_EVENT_WRITE,
                                PermissionType.APPOINTMENT_READ,
                                PermissionType.APPOINTMENT_WRITE,
                                PermissionType.EVENT_ATTENDANCE_READ,
                                PermissionType.EVENT_ATTENDANCE_WRITE,
                                PermissionType.USER_READ,
                                PermissionType.PROFILE_IMAGE_READ,
                                PermissionType.EMERGENCY_CONTACT_READ,
                                PermissionType.ABSENCE_READ,
                                PermissionType.DOCUMENT_READ,
                                PermissionType.TIME_ENTRY_READ,
                                PermissionType.WORKFLOW_READ,
                                PermissionType.TRAINING_COURSE_READ,
                                PermissionType.TRAINING_COURSE_WRITE,
                                PermissionType.DRIVING_LICENSE_READ,
                                PermissionType.DRIVING_LICENSE_WRITE,
                                PermissionType.COURSE_OFFERING_READ,
                                PermissionType.COURSE_OFFERING_WRITE)));
    }

    private static PermissionGroupBaseline permissionGroup(
            PermissionGroupType type, List<PermissionType> permissionTypes) {
        return new PermissionGroupBaseline(type.name(), null, null, type, permissionTypes);
    }

    private static PermissionGroupBaseline customPermissionGroup(
            String nameKey, String name, List<PermissionType> permissionTypes) {
        return new PermissionGroupBaseline(
                configKey(nameKey), name, null, PermissionGroupType.CUSTOM, permissionTypes);
    }

    private static List<GroupMappingBaseline> groupMappings() {
        return List.of(
                mapping(OrganizationGroupType.LEADERSHIP.name(), PermissionGroupType.ADMIN.name()),
                mapping(
                        OrganizationGroupType.ALL_MEMBERS.name(),
                        PermissionGroupType.STANDARD.name()),
                mapping(
                        configKey(ORG_GROUP_LEADERSHIP_TEAM_NAME_KEY),
                        configKey(PERM_GROUP_POWERUSER_NAME_KEY)),
                mapping(configKey(ORG_GROUP_HR_NAME_KEY), PermissionGroupType.ADMIN.name()));
    }

    private static String configKey(String nameKey) {
        return StandardConfigKey.customFromMessageKey(nameKey);
    }

    private static GroupMappingBaseline mapping(
            String orgGroupConfigKey, String permissionGroupConfigKey) {
        return new GroupMappingBaseline(orgGroupConfigKey, permissionGroupConfigKey);
    }

    private static List<WorkflowTypeMappingBaseline> workflowTypeMappings() {
        String hr = configKey(ORG_GROUP_HR_NAME_KEY);
        String leadership = OrganizationGroupType.LEADERSHIP.name();
        return List.of(
                workflowTypeMapping(WorkflowType.NAME_CHANGE, hr),
                workflowTypeMapping(WorkflowType.ADDRESS_CHANGE, hr),
                workflowTypeMapping(WorkflowType.CERTIFICATION_RECORD, hr),
                workflowTypeMapping(WorkflowType.TRAINING_COURSE_RECORD, hr),
                workflowTypeMapping(WorkflowType.DRIVING_LICENSE_RECORD, hr),
                workflowTypeMapping(WorkflowType.ORGANIZATION_JOIN_REQUEST, leadership),
                workflowTypeMapping(WorkflowType.MAIN_ORGANIZATION_CHANGE, leadership),
                workflowTypeMapping(WorkflowType.ORGANIZATION_LEAVE_REQUEST, leadership),
                workflowTypeMapping(WorkflowType.ORGANIZATION_GROUP_LEAVE_REQUEST, hr),
                workflowTypeMapping(WorkflowType.ORGANIZATION_GROUP_JOIN_REQUEST, hr),
                workflowTypeMapping(WorkflowType.ONBOARDING_SUBMISSION, hr),
                workflowTypeMapping(WorkflowType.ORGANIZATION_MEMBERSHIP_END_REQUEST, leadership),
                workflowTypeMapping(
                        WorkflowType.ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE, leadership),
                workflowTypeMapping(WorkflowType.USER_DOCUMENT_SUBMISSION, hr),
                workflowTypeMapping(WorkflowType.RANK_REQUEST, leadership),
                workflowTypeMapping(WorkflowType.COURSE_OFFERING_REGISTRATION, hr),
                workflowTypeMapping(WorkflowType.COURSE_REGISTRATION_CANCELLATION, hr));
    }

    private static WorkflowTypeMappingBaseline workflowTypeMapping(
            WorkflowType workflowType, String approverOrgGroupConfigKey) {
        return new WorkflowTypeMappingBaseline(
                String.valueOf(workflowType.getId()),
                workflowType.getId(),
                approverOrgGroupConfigKey,
                false,
                true,
                false);
    }

    private static List<PermissionType> permissionTypes(PermissionType... types) {
        return Arrays.asList(types);
    }

    private static List<PermissionType> assignablePermissionTypes() {
        return Arrays.stream(PermissionType.values())
                .filter(type -> type != PermissionType.ORGANIZATION_READ)
                .filter(type -> type != PermissionType.MANAGEMENT_ACCESS)
                .toList();
    }

    public record Labels(
            String orgGroupLeadershipTeam, String orgGroupHr, String permissionGroupPoweruser) {}
}
