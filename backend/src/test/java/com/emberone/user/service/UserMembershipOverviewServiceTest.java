/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.dto.OrganizationDto;
import com.emberone.organization.mapper.OrganizationMapper;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.domain.GroupMembershipType;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.organizationgroup.dto.OrganizationGroupDto;
import com.emberone.organizationgroup.mapper.OrganizationGroupMapper;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.security.OrganizationAccessService;
import com.emberone.user.domain.User;
import com.emberone.user.dto.OrganizationMembershipSectionDto;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.dto.UserOrganizationGroupMembershipDto;
import com.emberone.userorganizationgroupmembership.mapper.UserOrganizationGroupMembershipMapper;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.dto.UserOrganizationMembershipDto;
import com.emberone.userorganizationmembership.mapper.UserOrganizationMembershipMapper;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.organizationmembershipend.OrganizationMembershipEndRequestWorkflowOpenAccess;
import com.emberone.workflow.organizationusermainorganizationchange.OrganizationUserMainOrganizationChangeWorkflowOpenAccess;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserMembershipOverviewServiceTest {

    @Mock OrganizationRepository organizationRepository;
    @Mock UserRepository userRepository;
    @Mock UserOrganizationMembershipRepository organizationMembershipRepository;
    @Mock UserOrganizationGroupMembershipRepository groupMembershipRepository;
    @Mock UserOrganizationMembershipMapper organizationMembershipMapper;
    @Mock UserOrganizationGroupMembershipMapper groupMembershipMapper;
    @Mock OrganizationMapper organizationMapper;
    @Mock OrganizationGroupMapper organizationGroupMapper;
    @Mock OrganizationAccessService organizationAccessService;

    @Mock
    OrganizationMembershipEndRequestWorkflowOpenAccess
            organizationMembershipEndRequestWorkflowOpenAccess;

    @Mock
    OrganizationUserMainOrganizationChangeWorkflowOpenAccess
            organizationUserMainOrganizationChangeWorkflowOpenAccess;

    @InjectMocks UserMembershipOverviewService service;

    @Test
    void getMembershipOverviewReturnsOnlyContextOrganization() {
        Organization organization = organization(1L, "Alpha Org");
        User user = User.builder().id(10L).build();

        UserOrganizationMembership membership =
                orgMembership(
                        100L, user, organization, true, Instant.parse("2024-01-01T00:00:00Z"));

        OrganizationGroup group =
                OrganizationGroup.builder()
                        .id(50L)
                        .organization(organization)
                        .name("Führung")
                        .type(OrganizationGroupType.LEADERSHIP)
                        .archived(false)
                        .build();
        UserOrganizationGroupMembership groupMembership =
                UserOrganizationGroupMembership.builder()
                        .id(200L)
                        .user(user)
                        .organizationGroup(group)
                        .membershipType(GroupMembershipType.LEADER)
                        .startDate(Instant.parse("2024-01-01T00:00:00Z"))
                        .build();

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(organizationMembershipRepository.findByUser_IdAndOrganization_Id(10L, 1L))
                .thenReturn(List.of(membership));
        when(groupMembershipRepository.findByUser_IdAndOrganizationGroup_Organization_Id(10L, 1L))
                .thenReturn(List.of(groupMembership));

        when(organizationMapper.toDto(organization))
                .thenReturn(OrganizationDto.builder().id(1L).name("Alpha Org").build());
        when(organizationMembershipMapper.toDto(membership))
                .thenReturn(
                        UserOrganizationMembershipDto.builder()
                                .id(100L)
                                .main(true)
                                .startDate(Instant.parse("2024-01-01T00:00:00Z"))
                                .organization(
                                        OrganizationDto.builder().id(1L).name("Alpha Org").build())
                                .build());
        when(organizationAccessService.hasOrganizationPermission(1L, PermissionType.USER_WRITE))
                .thenReturn(true);
        when(organizationMembershipEndRequestWorkflowOpenAccess.canOpen(1L, 10L)).thenReturn(false);
        when(organizationUserMainOrganizationChangeWorkflowOpenAccess.canOpen(1L, 10L))
                .thenReturn(false);
        when(groupMembershipMapper.toDto(groupMembership))
                .thenReturn(
                        UserOrganizationGroupMembershipDto.builder()
                                .id(200L)
                                .organizationGroupId(50L)
                                .startDate(Instant.parse("2024-01-01T00:00:00Z"))
                                .membershipType(GroupMembershipType.LEADER)
                                .build());
        when(organizationGroupMapper.toDto(group))
                .thenReturn(
                        OrganizationGroupDto.builder()
                                .id(50L)
                                .organizationId(1L)
                                .name("Führung")
                                .type(OrganizationGroupType.LEADERSHIP)
                                .build());

        OrganizationMembershipSectionDto overview = service.getMembershipOverview(1L, 10L);

        assertThat(overview.getOrganization().getName()).isEqualTo("Alpha Org");
        assertThat(overview.isCanWrite()).isTrue();
        assertThat(overview.getOrganizationMemberships()).hasSize(1);
        assertThat(overview.getGroups()).hasSize(1);
        assertThat(overview.getGroups().getFirst().getGroup().getName()).isEqualTo("Führung");
        assertThat(overview.isInLeadershipGroup()).isTrue();
    }

    @Test
    void getMembershipOverviewDoesNotExposeWorkflowActionsWithoutUserWrite() {
        Organization organization = organization(1L, "Alpha Org");
        User user = User.builder().id(10L).build();

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(organizationMembershipRepository.findByUser_IdAndOrganization_Id(10L, 1L))
                .thenReturn(List.of());
        when(groupMembershipRepository.findByUser_IdAndOrganizationGroup_Organization_Id(10L, 1L))
                .thenReturn(List.of());
        when(organizationMapper.toDto(organization))
                .thenReturn(OrganizationDto.builder().id(1L).name("Alpha Org").build());
        when(organizationAccessService.hasOrganizationPermission(1L, PermissionType.USER_WRITE))
                .thenReturn(false);

        OrganizationMembershipSectionDto overview = service.getMembershipOverview(1L, 10L);

        assertThat(overview.isCanRequestOrganizationMembershipEnd()).isFalse();
        assertThat(overview.isCanRequestOrganizationUserMainOrganizationChange()).isFalse();
    }

    @Test
    void getMembershipOverviewReturnsEmptyListsWhenUserHasNoMembershipsInOrganization() {
        Organization organization = organization(1L, "Alpha Org");
        User user = User.builder().id(10L).build();

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(organizationMembershipRepository.findByUser_IdAndOrganization_Id(10L, 1L))
                .thenReturn(List.of());
        when(groupMembershipRepository.findByUser_IdAndOrganizationGroup_Organization_Id(10L, 1L))
                .thenReturn(List.of());
        when(organizationMapper.toDto(organization))
                .thenReturn(OrganizationDto.builder().id(1L).name("Alpha Org").build());
        when(organizationAccessService.hasOrganizationPermission(1L, PermissionType.USER_WRITE))
                .thenReturn(false);

        OrganizationMembershipSectionDto overview = service.getMembershipOverview(1L, 10L);

        assertThat(overview.getOrganization().getId()).isEqualTo(1L);
        assertThat(overview.isCanWrite()).isFalse();
        assertThat(overview.isCanRequestOrganizationMembershipEnd()).isFalse();
        assertThat(overview.getOrganizationMemberships()).isEmpty();
        assertThat(overview.getGroups()).isEmpty();
        assertThat(overview.isInLeadershipGroup()).isFalse();
    }

    private static Organization organization(Long id, String name) {
        return Organization.builder().id(id).name(name).build();
    }

    private static UserOrganizationMembership orgMembership(
            Long id, User user, Organization organization, boolean main, Instant startDate) {
        return UserOrganizationMembership.builder()
                .id(id)
                .user(user)
                .organization(organization)
                .main(main)
                .startDate(startDate)
                .build();
    }
}
