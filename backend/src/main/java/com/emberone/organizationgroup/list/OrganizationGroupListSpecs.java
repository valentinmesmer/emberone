/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.list;

import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import org.springframework.data.jpa.domain.Specification;

public final class OrganizationGroupListSpecs {

    private OrganizationGroupListSpecs() {}

    public static Specification<OrganizationGroup> forOrganization(Long organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organization").get("id"), organizationId);
    }

    public static Specification<OrganizationGroup> userInterfaceVisible() {
        return (root, query, cb) ->
                root.get("type").in(OrganizationGroupType.userInterfaceVisibleTypes());
    }

    public static Specification<OrganizationGroup> statusFilter(
            OrganizationGroupListStatus status) {
        return switch (status) {
            case ACTIVE -> (root, query, cb) -> cb.isFalse(root.get("archived"));
            case ARCHIVED -> (root, query, cb) -> cb.isTrue(root.get("archived"));
            case ALL -> (root, query, cb) -> cb.conjunction();
        };
    }
}
