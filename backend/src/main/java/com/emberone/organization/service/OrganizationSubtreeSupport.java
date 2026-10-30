/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.service;

import com.emberone.organization.domain.Organization;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Collects organization subtree ids from a flat tenant org list. */
public final class OrganizationSubtreeSupport {

    private OrganizationSubtreeSupport() {}

    public static Set<Long> collectSubtreeIds(
            long rootOrganizationId, List<Organization> organizationsInTenant) {
        Map<Long, List<Long>> childrenByParentId = new HashMap<>();
        for (Organization organization : organizationsInTenant) {
            if (organization.getParentOrganization() != null) {
                childrenByParentId
                        .computeIfAbsent(
                                organization.getParentOrganization().getId(),
                                ignored -> new ArrayList<>())
                        .add(organization.getId());
            }
        }

        Set<Long> result = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(rootOrganizationId);
        while (!queue.isEmpty()) {
            Long current = queue.removeFirst();
            if (!result.add(current)) {
                continue;
            }
            for (Long childId : childrenByParentId.getOrDefault(current, List.of())) {
                queue.addLast(childId);
            }
        }
        return result;
    }

    /**
     * Whether the organization is the ancestor itself or reaches it through its parent links. Works
     * on a partial list, since it only follows the links; cyclic links cannot make it loop.
     */
    public static boolean isSelfOrDescendant(Organization organization, long ancestorId) {
        Set<Long> seen = new HashSet<>();
        for (Organization current = organization;
                current != null && seen.add(current.getId());
                current = current.getParentOrganization()) {
            if (current.getId() == ancestorId) {
                return true;
            }
        }
        return false;
    }

    static boolean isDescendantOf(
            long candidateDescendantId, long ancestorId, List<Organization> organizationsInTenant) {
        return collectSubtreeIds(ancestorId, organizationsInTenant).contains(candidateDescendantId);
    }
}
