/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.organization.domain.Organization;
import com.emberone.organizationgroup.domain.GroupMembershipType;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.user.dto.SelfMembershipHistoryDto;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SelfMembershipHistoryServiceTest {

    private static final long USER_ID = 1L;

    @Mock UserOrganizationMembershipRepository organizationMembershipRepository;
    @Mock UserOrganizationGroupMembershipRepository groupMembershipRepository;

    @InjectMocks SelfMembershipHistoryService service;

    @Test
    void returnsCurrentAndPastPeriodsAndSkipsHiddenGroupsAndFutureStarts() {
        Instant now = Instant.now();
        Organization organization =
                Organization.builder().id(10L).name("FF").organizationCode("FF").build();
        when(organizationMembershipRepository.findByUser_Id(USER_ID))
                .thenReturn(
                        List.of(
                                UserOrganizationMembership.builder()
                                        .organization(organization)
                                        .main(true)
                                        .startDate(now.minus(10, ChronoUnit.DAYS))
                                        .build(),
                                UserOrganizationMembership.builder()
                                        .organization(organization)
                                        .main(false)
                                        .startDate(now.minus(900, ChronoUnit.DAYS))
                                        .endDate(now.minus(500, ChronoUnit.DAYS))
                                        .build()));
        OrganizationGroup lead =
                group(20L, "Führung", OrganizationGroupType.LEADERSHIP, organization);
        OrganizationGroup all = group(21L, "Alle", OrganizationGroupType.ALL_MEMBERS, organization);
        when(groupMembershipRepository.findByUser_Id(USER_ID))
                .thenReturn(
                        List.of(
                                groupMembership(
                                        lead,
                                        GroupMembershipType.LEADER,
                                        now.minus(5, ChronoUnit.DAYS),
                                        null),
                                groupMembership(
                                        lead,
                                        GroupMembershipType.DEPUTY_LEADER,
                                        now.minus(50, ChronoUnit.DAYS),
                                        now.minus(5, ChronoUnit.DAYS)),
                                groupMembership(
                                        all,
                                        GroupMembershipType.MEMBER,
                                        now.minus(50, ChronoUnit.DAYS),
                                        null),
                                groupMembership(
                                        lead,
                                        GroupMembershipType.MEMBER,
                                        now.plus(5, ChronoUnit.DAYS),
                                        null)));

        SelfMembershipHistoryDto history = service.getForUser(USER_ID);

        assertThat(history.getOrganizations()).hasSize(2);
        assertThat(history.getOrganizations().get(0).isActive()).isTrue();
        assertThat(history.getOrganizations().get(1).isActive()).isFalse();
        assertThat(history.getGroups()).hasSize(2);
        assertThat(history.getGroups().get(0).getMembershipType())
                .isEqualTo(GroupMembershipType.LEADER);
        assertThat(history.getGroups().get(0).isActive()).isTrue();
        assertThat(history.getGroups().get(1).getMembershipType())
                .isEqualTo(GroupMembershipType.DEPUTY_LEADER);
        assertThat(history.getGroups().get(1).isActive()).isFalse();
    }

    private static OrganizationGroup group(
            long id, String name, OrganizationGroupType type, Organization organization) {
        return OrganizationGroup.builder()
                .id(id)
                .name(name)
                .type(type)
                .organization(organization)
                .build();
    }

    private static UserOrganizationGroupMembership groupMembership(
            OrganizationGroup group, GroupMembershipType type, Instant start, Instant end) {
        return UserOrganizationGroupMembership.builder()
                .organizationGroup(group)
                .membershipType(type)
                .startDate(start)
                .endDate(end)
                .build();
    }
}
