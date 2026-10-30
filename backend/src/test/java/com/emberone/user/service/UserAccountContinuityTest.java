/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.emberone.onboarding.domain.OnboardingInvitationStatus;
import com.emberone.onboarding.repository.OnboardingInvitationRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserAccountContinuityTest {

    @Mock UserRepository userRepository;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock OnboardingInvitationRepository onboardingInvitationRepository;

    @InjectMocks UserAccountContinuity userAccountContinuity;

    @Test
    void remainsSustainableWithTwoFutureMembershipEndsWhenAnotherUnboundedMembershipExists() {
        Long userId = 10L;
        User user = User.builder().id(userId).tenantId(1L).build();
        Organization orgA = Organization.builder().id(1L).tenantId(1L).name("A").build();
        Organization orgB = Organization.builder().id(2L).tenantId(1L).name("B").build();
        Organization orgC = Organization.builder().id(3L).tenantId(1L).name("C").build();

        Instant endA = Instant.parse("2026-12-01T00:00:00Z");
        Instant endB = Instant.parse("2026-12-15T00:00:00Z");

        UserOrganizationMembership membershipA =
                membership(userId, orgA, true, Instant.parse("2024-01-01T00:00:00Z"), endA);
        UserOrganizationMembership membershipB =
                membership(userId, orgB, false, Instant.parse("2024-01-01T00:00:00Z"), endB);
        UserOrganizationMembership membershipC =
                membership(userId, orgC, false, Instant.parse("2024-01-01T00:00:00Z"), null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(membershipRepository.findActiveByUser_Id(eq(userId), any(Instant.class)))
                .thenReturn(List.of(membershipA, membershipB, membershipC));

        assertThat(
                        userAccountContinuity
                                .wouldRemainSustainableAfterEndingOrganizationMembership(
                                        userId, 1L, endA))
                .isTrue();
        assertThat(
                        userAccountContinuity
                                .wouldRemainSustainableAfterEndingOrganizationMembership(
                                        userId, 2L, endB))
                .isTrue();
    }

    @Test
    void notSustainableWhenOnlyBoundedMembershipsWouldRemain() {
        Long userId = 10L;
        User user = User.builder().id(userId).tenantId(1L).build();
        Organization orgA = Organization.builder().id(1L).tenantId(1L).name("A").build();
        Instant endA = Instant.parse("2026-12-01T00:00:00Z");

        UserOrganizationMembership membershipA =
                membership(userId, orgA, true, Instant.parse("2024-01-01T00:00:00Z"), endA);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(membershipRepository.findActiveByUser_Id(eq(userId), any(Instant.class)))
                .thenReturn(List.of(membershipA));
        when(onboardingInvitationRepository.findByUserIdAndStatusIn(
                        userId, OnboardingInvitationStatus.pending()))
                .thenReturn(List.of());

        assertThat(
                        userAccountContinuity
                                .wouldRemainSustainableAfterEndingOrganizationMembership(
                                        userId, 1L, endA))
                .isFalse();
    }

    private static UserOrganizationMembership membership(
            Long userId, Organization organization, boolean main, Instant start, Instant end) {
        return UserOrganizationMembership.builder()
                .id(organization.getId())
                .user(User.builder().id(userId).build())
                .organization(organization)
                .main(main)
                .startDate(start)
                .endDate(end)
                .build();
    }
}
