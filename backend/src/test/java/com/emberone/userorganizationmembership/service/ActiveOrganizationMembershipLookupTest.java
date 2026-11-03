/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.userorganizationmembership.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.emberone.organization.domain.Organization;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActiveOrganizationMembershipLookupTest {

    private static final long USER_ID = 5L;

    @Mock UserOrganizationMembershipRepository membershipRepository;

    private ActiveOrganizationMembershipLookup lookup;

    @BeforeEach
    void setUp() {
        lookup = new ActiveOrganizationMembershipLookup(membershipRepository);
    }

    @Test
    void listActiveOrganizationIds_returnsDistinctActiveMembershipOrgs() {
        when(membershipRepository.findActiveByUser_Id(eq(USER_ID), any(Instant.class)))
                .thenReturn(List.of(membership(10L), membership(20L), membership(10L)));

        assertThat(lookup.listActiveOrganizationIds(USER_ID)).containsExactly(10L, 20L);
    }

    @Test
    void listActiveOrganizationIds_returnsEmptyWithoutMemberships() {
        when(membershipRepository.findActiveByUser_Id(eq(USER_ID), any(Instant.class)))
                .thenReturn(List.of());

        assertThat(lookup.listActiveOrganizationIds(USER_ID)).isEmpty();
    }

    private static UserOrganizationMembership membership(long organizationId) {
        return UserOrganizationMembership.builder()
                .organization(Organization.builder().id(organizationId).build())
                .startDate(Instant.now())
                .build();
    }
}
