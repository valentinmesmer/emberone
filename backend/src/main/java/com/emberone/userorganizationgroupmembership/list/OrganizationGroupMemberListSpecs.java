/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.userorganizationgroupmembership.list;

import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

public final class OrganizationGroupMemberListSpecs {

    private OrganizationGroupMemberListSpecs() {}

    public static Specification<UserOrganizationGroupMembership> forOrganizationGroup(
            Long organizationGroupId) {
        return (root, query, cb) ->
                cb.equal(root.get("organizationGroup").get("id"), organizationGroupId);
    }

    public static Specification<UserOrganizationGroupMembership> activeAt(Instant at) {
        return (root, query, cb) ->
                cb.and(
                        cb.lessThanOrEqualTo(root.get("startDate"), at),
                        cb.or(
                                cb.isNull(root.get("endDate")),
                                cb.greaterThanOrEqualTo(root.get("endDate"), at)));
    }
}
