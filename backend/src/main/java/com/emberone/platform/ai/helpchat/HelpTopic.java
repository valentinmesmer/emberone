/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai.helpchat;

import com.emberone.permission.domain.PermissionType;
import lombok.Getter;

/**
 * Pages the help chat can explain and link to, besides startable requests (workflow types). The
 * model only picks a key; who may see a topic and which organizations it applies to is decided here
 * from the user's effective permissions, the link is built by the frontend from a fixed map
 * (concept rule R3). Help texts live in {@code resources/ai/help/topics/<KEY>.md}.
 *
 * <ul>
 *   <li>{@link HelpTopicArea#SELF}: every self-service user ("Mein Bereich").
 *   <li>{@link HelpTopicArea#ORGANIZATION}: organizations where the user has {@link
 *       #readPermission}; creating needs {@link #createPermission} in that organization.
 *   <li>{@link HelpTopicArea#ADMIN}: tenant or global admins; tenant pages need a tenant in the
 *       token.
 * </ul>
 */
@Getter
public enum HelpTopic {
    // My area (order of the hub: inbox, time, training, profile, account)
    MY_TODOS(HelpTopicArea.SELF, null, null, false, false),
    MY_WORKFLOWS(HelpTopicArea.SELF, null, null, false, false),
    MY_NOTIFICATIONS(HelpTopicArea.SELF, null, null, false, false),
    MY_TIME_ENTRIES(HelpTopicArea.SELF, null, null, true, false),
    MY_ABSENCES(HelpTopicArea.SELF, null, null, true, false),
    MY_COURSE_REGISTRATIONS(HelpTopicArea.SELF, null, null, false, false),
    MY_QUALIFICATIONS(HelpTopicArea.SELF, null, null, false, false),
    MY_CERTIFICATIONS(HelpTopicArea.SELF, null, null, false, false),
    MY_TRAINING_COURSES(HelpTopicArea.SELF, null, null, false, false),
    MY_DRIVING_LICENSES(HelpTopicArea.SELF, null, null, false, false),
    MY_MASTER_DATA(HelpTopicArea.SELF, null, null, false, false),
    MY_RANK(HelpTopicArea.SELF, null, null, false, false),
    MY_EMERGENCY_CONTACTS(HelpTopicArea.SELF, null, null, true, false),
    MY_DOCUMENTS(HelpTopicArea.SELF, null, null, false, false),
    MY_NOTIFICATION_SETTINGS(HelpTopicArea.SELF, null, null, false, false),

    // Organization (order of the navigation; the last two have no hub tile)
    ORG_APPOINTMENTS(
            HelpTopicArea.ORGANIZATION,
            PermissionType.APPOINTMENT_READ,
            PermissionType.APPOINTMENT_WRITE,
            false,
            false),
    ORG_OPERATION_EVENTS(
            HelpTopicArea.ORGANIZATION,
            PermissionType.OPERATION_EVENT_READ,
            PermissionType.OPERATION_EVENT_WRITE,
            false,
            false),
    ORG_SERVICE_EVENTS(
            HelpTopicArea.ORGANIZATION,
            PermissionType.SERVICE_EVENT_READ,
            PermissionType.SERVICE_EVENT_WRITE,
            false,
            false),
    ORG_USERS(HelpTopicArea.ORGANIZATION, PermissionType.USER_READ, null, false, false),
    ORG_ABSENCES(HelpTopicArea.ORGANIZATION, PermissionType.ABSENCE_READ, null, false, false),
    ORG_RANKS(HelpTopicArea.ORGANIZATION, PermissionType.RANK_READ, null, false, false),
    ORG_GROUPS(
            HelpTopicArea.ORGANIZATION, PermissionType.ORGANIZATION_GROUP_READ, null, false, false),
    ORG_QUALIFICATIONS(
            HelpTopicArea.ORGANIZATION, PermissionType.QUALIFICATION_READ, null, false, false),
    ORG_CERTIFICATIONS(
            HelpTopicArea.ORGANIZATION, PermissionType.CERTIFICATION_READ, null, false, false),
    ORG_COURSE_OFFERINGS(
            HelpTopicArea.ORGANIZATION,
            PermissionType.COURSE_OFFERING_READ,
            PermissionType.COURSE_OFFERING_WRITE,
            false,
            false),
    ORG_COURSE_REGISTRATIONS(
            HelpTopicArea.ORGANIZATION, PermissionType.TRAINING_COURSE_READ, null, false, false),
    ORG_WORKFLOWS(HelpTopicArea.ORGANIZATION, PermissionType.WORKFLOW_READ, null, false, false),
    ORG_MANAGEMENT(
            HelpTopicArea.ORGANIZATION, PermissionType.MANAGEMENT_ACCESS, null, false, false),
    ORG_EMERGENCY_CONTACTS(
            HelpTopicArea.ORGANIZATION, PermissionType.EMERGENCY_CONTACT_READ, null, false, false),
    ORG_TIME_ENTRIES(
            HelpTopicArea.ORGANIZATION, PermissionType.TIME_ENTRY_READ, null, false, false),

    // Admin
    ADMIN_TENANT(HelpTopicArea.ADMIN, null, null, false, true),
    ADMIN_ORGANIZATIONS(HelpTopicArea.ADMIN, null, null, false, true),
    ADMIN_STANDARDS(HelpTopicArea.ADMIN, null, null, false, true),
    ADMIN_CATALOGS(HelpTopicArea.ADMIN, null, null, false, true),
    ADMIN_RETENTION(HelpTopicArea.ADMIN, null, null, false, true),
    ADMIN_USERS(HelpTopicArea.ADMIN, null, null, false, false);

    private final HelpTopicArea area;

    /** Organization topics: permission needed to see the page in an organization. */
    private final PermissionType readPermission;

    /** Organization topics: permission needed to create there; null = no create link. */
    private final PermissionType createPermission;

    /** Self topics: the page has a create dialog the link can open. */
    private final boolean selfCreate;

    /** Admin topics: the page belongs to the tenant from the token. */
    private final boolean tenantScoped;

    HelpTopic(
            HelpTopicArea area,
            PermissionType readPermission,
            PermissionType createPermission,
            boolean selfCreate,
            boolean tenantScoped) {
        this.area = area;
        this.readPermission = readPermission;
        this.createPermission = createPermission;
        this.selfCreate = selfCreate;
        this.tenantScoped = tenantScoped;
    }

    /** Whether the page has a create dialog the link can open (permission checked separately). */
    public boolean supportsCreate() {
        return selfCreate || createPermission != null;
    }
}
