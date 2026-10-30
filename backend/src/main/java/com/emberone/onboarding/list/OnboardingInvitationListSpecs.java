/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.list;

import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboarding.domain.OnboardingInvitationStatus;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

public final class OnboardingInvitationListSpecs {

    private OnboardingInvitationListSpecs() {}

    public static Specification<OnboardingInvitation> forOrganization(Long organizationId) {
        return (root, query, cb) -> cb.equal(root.get("targetOrganizationId"), organizationId);
    }

    public static Specification<OnboardingInvitation> pendingStatuses() {
        return (root, query, cb) -> root.get("status").in(OnboardingInvitationStatus.pending());
    }

    public static Specification<OnboardingInvitation> withActiveUser() {
        return (root, query, cb) -> {
            var user = root.join("user", JoinType.INNER);
            return cb.isNull(user.get("deactivatedAt"));
        };
    }

    public static Specification<OnboardingInvitation> withoutActiveOrganizationMembership(
            Long organizationId) {
        Instant now = Instant.now();
        return (root, query, cb) -> {
            Subquery<Long> membership = query.subquery(Long.class);
            Root<UserOrganizationMembership> membershipRoot =
                    membership.from(UserOrganizationMembership.class);
            membership.select(cb.literal(1L));
            membership.where(
                    cb.equal(membershipRoot.get("user").get("id"), root.get("userId")),
                    cb.equal(membershipRoot.get("organization").get("id"), organizationId),
                    cb.lessThanOrEqualTo(membershipRoot.get("startDate"), now),
                    cb.or(
                            cb.isNull(membershipRoot.get("endDate")),
                            cb.greaterThanOrEqualTo(membershipRoot.get("endDate"), now)));
            return cb.not(cb.exists(membership));
        };
    }
}
