/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.list;

import com.emberone.user.domain.User;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

public final class OrganizationMemberListSpecs {

    private OrganizationMemberListSpecs() {}

    public static Specification<User> activeInOrganization(Long organizationId) {
        Instant now = Instant.now();
        return (root, query, cb) -> {
            Subquery<Long> membership = query.subquery(Long.class);
            Root<UserOrganizationMembership> membershipRoot =
                    membership.from(UserOrganizationMembership.class);
            membership.select(cb.literal(1L));
            membership.where(
                    cb.equal(membershipRoot.get("user"), root),
                    cb.equal(membershipRoot.get("organization").get("id"), organizationId),
                    cb.lessThanOrEqualTo(membershipRoot.get("startDate"), now),
                    cb.or(
                            cb.isNull(membershipRoot.get("endDate")),
                            cb.greaterThanOrEqualTo(membershipRoot.get("endDate"), now)));
            return cb.exists(membership);
        };
    }

    public static Specification<User> notDeactivated() {
        return (root, query, cb) -> cb.isNull(root.get("deactivatedAt"));
    }
}
