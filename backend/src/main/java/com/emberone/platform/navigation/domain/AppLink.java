/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.navigation.domain;

/**
 * Semantic in-app link target. Persist as {@code link_type} + {@code link_id}; resolve {@code
 * navigationPath} at read time via {@code AppNavigationResolver}.
 */
public record AppLink(AppLinkType type, long linkId) {

    public AppLink {
        if (type == null) {
            throw new IllegalArgumentException("link type is required");
        }
        if (linkId <= 0) {
            throw new IllegalArgumentException("link id must be positive");
        }
    }

    public static AppLink workflow(long workflowId) {
        return new AppLink(AppLinkType.WORKFLOW, workflowId);
    }
}
