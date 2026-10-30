/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.spi;

/**
 * Stable lock keys for {@link WorkflowHandler#resolveOpenExclusivityKeys}: at most one active
 * workflow per tenant may hold a given key, across all workflow types that share the same business
 * operation.
 */
public final class WorkflowOpenExclusivityKeys {

    private static final String MEMBERSHIP_EXIT_PREFIX = "membership-exit:user:";
    private static final String MAIN_ORGANIZATION_PREFIX = "main-organization:user:";

    private WorkflowOpenExclusivityKeys() {}

    /** Ending membership of {@code userId} in {@code organizationId} (self or org-initiated). */
    public static String organizationMembershipExit(long userId, long organizationId) {
        return MEMBERSHIP_EXIT_PREFIX + userId + ":org:" + organizationId;
    }

    /** Changing the main organization of {@code userId} (self or org-initiated). */
    public static String userMainOrganizationChange(long userId) {
        return MAIN_ORGANIZATION_PREFIX + userId;
    }
}
