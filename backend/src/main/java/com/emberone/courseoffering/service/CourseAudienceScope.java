/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import java.util.Map;
import java.util.Set;

/**
 * What a user is, audience-wise: organizations with an active membership, those organizations plus
 * all their ancestors (a subtree audience matches via any of them), their organization types, and
 * the tree depth of every organization in the ancestor set (root = 0), which ranks subtree entries
 * by specificity.
 */
public record CourseAudienceScope(
        Set<Long> memberOrganizationIds,
        Set<Long> ancestorOrSelfOrganizationIds,
        Set<Long> organizationTypeIds,
        Map<Long, Integer> depthByOrganizationId) {

    public CourseAudienceScope(
            Set<Long> memberOrganizationIds,
            Set<Long> ancestorOrSelfOrganizationIds,
            Set<Long> organizationTypeIds) {
        this(memberOrganizationIds, ancestorOrSelfOrganizationIds, organizationTypeIds, Map.of());
    }
}
