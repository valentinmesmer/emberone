/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.domain;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public enum OrganizationGroupType {
    LEADERSHIP,
    ALL_MEMBERS,
    CUSTOM;

    private static final Set<OrganizationGroupType> HIDDEN_FROM_USER_INTERFACE =
            EnumSet.of(ALL_MEMBERS);

    public static List<OrganizationGroupType> userInterfaceVisibleTypes() {
        return List.of(values()).stream()
                .filter(OrganizationGroupType::isVisibleInUserInterface)
                .toList();
    }

    public boolean isSystem() {
        return this != CUSTOM;
    }

    /**
     * Whether this group type may be assigned as a workflow approver. All-members groups are
     * excluded because every user belongs to them.
     */
    public boolean canApproveWorkflows() {
        return this != ALL_MEMBERS;
    }

    /** Standard function grants apply to leadership and custom org groups only. */
    public boolean supportsFunctionGrants() {
        return this == LEADERSHIP || this == CUSTOM;
    }

    /**
     * Whether this group type is exposed in user-facing APIs. All-members groups are managed
     * automatically and must not appear in the frontend.
     */
    public boolean isVisibleInUserInterface() {
        return !HIDDEN_FROM_USER_INTERFACE.contains(this);
    }

    public String getMessageKey() {
        return "organizationGroup." + name();
    }
}
