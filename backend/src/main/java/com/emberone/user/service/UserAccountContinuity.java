/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.service;

import com.emberone.onboarding.domain.OnboardingInvitationStatus;
import com.emberone.onboarding.repository.OnboardingInvitationRepository;
import com.emberone.user.domain.User;
import com.emberone.user.exception.UserAccountSustainabilityException;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationmembership.domain.MembershipTimestampSupport;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.exception.WorkflowValidationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures active users remain anchored: unbounded org membership, pending onboarding (invite-only),
 * or an approved deactivation path.
 */
@Service
@RequiredArgsConstructor
public class UserAccountContinuity {

    private final UserRepository userRepository;
    private final UserOrganizationMembershipRepository membershipRepository;
    private final OnboardingInvitationRepository onboardingInvitationRepository;

    @Transactional(readOnly = true)
    public void requireSustainableAfterEndingOrganizationMembership(
            Long userId, Long organizationId, Instant endDate) {
        if (!wouldRemainSustainableAfterEndingOrganizationMembership(
                userId, organizationId, endDate)) {
            throw new UserAccountSustainabilityException(
                    "Ending this organization membership would leave the user without an organization"
                            + " anchor; leave via the main organization instead");
        }
    }

    @Transactional(readOnly = true)
    public void requireSustainableAfterEndingOrganizationMembershipForWorkflow(
            Long userId, Long organizationId, Instant endDate) {
        if (!wouldRemainSustainableAfterEndingOrganizationMembership(
                userId, organizationId, endDate)) {
            throw new WorkflowValidationException(
                    "Ending this organization membership would leave the user without an organization"
                            + " anchor; leave via the main organization instead",
                    WorkflowValidationException.CODE_USER_ACCOUNT_NOT_SUSTAINABLE);
        }
    }

    @Transactional(readOnly = true)
    public boolean wouldRemainSustainableAfterEndingOrganizationMembership(
            Long userId, Long organizationId, Instant endDate) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));
        Map<Long, Instant> overrides = new HashMap<>();
        overrides.put(organizationId, endDate);
        Instant afterEnd = endDate.plusMillis(1);
        return isSustainableAt(user, userId, afterEnd, overrides);
    }

    private boolean isSustainableAt(
            User user, Long userId, Instant at, Map<Long, Instant> endOverridesByOrganizationId) {
        if (user.getDeactivatedAt() != null && !user.getDeactivatedAt().isAfter(at)) {
            return true;
        }
        List<UserOrganizationMembership> memberships =
                membershipRepository.findActiveByUser_Id(userId, Instant.now());
        boolean hasUnboundedActive =
                memberships.stream()
                        .anyMatch(
                                m ->
                                        isActiveAtMembership(m, at, endOverridesByOrganizationId)
                                                && effectiveEndDate(m, endOverridesByOrganizationId)
                                                        == null);

        if (hasUnboundedActive) {
            return true;
        }

        boolean hasAnyActiveAt =
                memberships.stream()
                        .anyMatch(m -> isActiveAtMembership(m, at, endOverridesByOrganizationId));
        if (!hasAnyActiveAt && hasPendingOnboarding(userId)) {
            return true;
        }

        return false;
    }

    private boolean hasPendingOnboarding(Long userId) {
        return !onboardingInvitationRepository
                .findByUserIdAndStatusIn(userId, OnboardingInvitationStatus.pending())
                .isEmpty();
    }

    private static boolean isActiveAtMembership(
            UserOrganizationMembership membership,
            Instant at,
            Map<Long, Instant> endOverridesByOrganizationId) {
        Long organizationId = membership.getOrganization().getId();
        Instant end =
                endOverridesByOrganizationId.containsKey(organizationId)
                        ? endOverridesByOrganizationId.get(organizationId)
                        : membership.getEndDate();
        return MembershipTimestampSupport.isActiveAt(membership.getStartDate(), end, at);
    }

    private static Instant effectiveEndDate(
            UserOrganizationMembership membership,
            Map<Long, Instant> endOverridesByOrganizationId) {
        Long organizationId = membership.getOrganization().getId();
        return endOverridesByOrganizationId.containsKey(organizationId)
                ? endOverridesByOrganizationId.get(organizationId)
                : membership.getEndDate();
    }
}
