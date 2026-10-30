/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.user.list;

import com.emberone.identity.domain.IdentityUser;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.user.domain.User;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.util.Collection;
import org.springframework.data.jpa.domain.Specification;

public final class AdminUserListSpecs {

    private AdminUserListSpecs() {}

    /** Enabled accounts whose user master record is not deactivated. */
    public static Specification<IdentityUser> active() {
        return (root, query, cb) -> {
            Subquery<Long> activeUser = query.subquery(Long.class);
            Root<User> user = activeUser.from(User.class);
            activeUser
                    .select(user.get("id"))
                    .where(
                            cb.equal(user.get("id"), root.get("emberoneUserId")),
                            cb.equal(user.get("tenantId"), root.get("tenantId")),
                            cb.isNull(user.get("deactivatedAt")));
            return cb.and(cb.isTrue(root.get("accountEnabled")), cb.exists(activeUser));
        };
    }

    public static Specification<IdentityUser> inTenants(Collection<Long> tenantIds) {
        return (root, query, cb) -> root.get("tenantId").in(tenantIds);
    }

    public static Specification<IdentityUser> withUserId(long userId) {
        return (root, query, cb) -> cb.equal(root.get("emberoneUserId"), userId);
    }

    public static Specification<IdentityUser> withAppRoles(Collection<EmberOneAppRole> appRoles) {
        return (root, query, cb) ->
                root.get("appRole")
                        .in(appRoles.stream().map(EmberOneAppRole::getRoleName).toList());
    }

    /** Users with an active membership in at least one of the given organizations. */
    public static Specification<IdentityUser> activeMemberOfOrganizations(
            Collection<Long> organizationIds) {
        Instant now = Instant.now();
        return (root, query, cb) -> {
            Subquery<Long> membership = query.subquery(Long.class);
            Root<UserOrganizationMembership> member =
                    membership.from(UserOrganizationMembership.class);
            membership
                    .select(cb.literal(1L))
                    .where(
                            cb.equal(member.get("user").get("id"), root.get("emberoneUserId")),
                            cb.equal(member.get("tenantId"), root.get("tenantId")),
                            member.get("organization").get("id").in(organizationIds),
                            cb.lessThanOrEqualTo(member.get("startDate"), now),
                            cb.or(
                                    cb.isNull(member.get("endDate")),
                                    cb.greaterThanOrEqualTo(member.get("endDate"), now)));
            return cb.exists(membership);
        };
    }

    /** Applies the optional filters of {@link AdminUserListFilter}. */
    public static Specification<IdentityUser> matching(AdminUserListFilter filter) {
        Specification<IdentityUser> spec = (root, query, cb) -> cb.conjunction();
        if (!filter.tenantIds().isEmpty()) {
            spec = spec.and(inTenants(filter.tenantIds()));
        }
        if (!filter.organizationIds().isEmpty()) {
            spec = spec.and(activeMemberOfOrganizations(filter.organizationIds()));
        }
        if (!filter.appRoles().isEmpty()) {
            spec = spec.and(withAppRoles(filter.appRoles()));
        }
        return spec;
    }
}
