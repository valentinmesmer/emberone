/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequestScopedOpenerMembershipsTest {

    private static final Long USER_ID = 10L;

    @Mock private UserOrganizationMembershipRepository membershipRepository;

    @InjectMocks private RequestScopedOpenerMemberships memberships;

    @Test
    void activeMembershipIsQueriedOncePerRequest() {
        when(membershipRepository.existsActiveByUser_Id(eq(USER_ID), any(Instant.class)))
                .thenReturn(true);

        assertThat(memberships.hasActiveMembership(USER_ID)).isTrue();
        assertThat(memberships.hasActiveMembership(USER_ID)).isTrue();

        verify(membershipRepository, times(1)).existsActiveByUser_Id(eq(USER_ID), any());
    }

    @Test
    void mainMembershipIsQueriedOncePerRequestEvenWhenAbsent() {
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(USER_ID), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThat(memberships.hasActiveMainMembership(USER_ID)).isFalse();
        assertThat(memberships.hasActiveMainMembership(USER_ID)).isFalse();

        verify(membershipRepository, times(1)).findActiveByUser_IdAndMainTrue(eq(USER_ID), any());
    }

    @Test
    void presentMainMembershipIsReported() {
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(USER_ID), any(Instant.class)))
                .thenReturn(Optional.of(UserOrganizationMembership.builder().build()));

        assertThat(memberships.hasActiveMainMembership(USER_ID)).isTrue();
    }
}
