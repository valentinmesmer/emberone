/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.list;

import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

public final class OrganizationRankMemberListSpecs {

    private OrganizationRankMemberListSpecs() {}

    public static Specification<UserOrganizationMembership> activeInOrganization(
            long organizationId) {
        Instant now = Instant.now();
        return (root, query, cb) ->
                cb.and(
                        cb.equal(root.get("organization").get("id"), organizationId),
                        cb.lessThanOrEqualTo(root.get("startDate"), now),
                        cb.or(
                                cb.isNull(root.get("endDate")),
                                cb.greaterThanOrEqualTo(root.get("endDate"), now)));
    }
}
