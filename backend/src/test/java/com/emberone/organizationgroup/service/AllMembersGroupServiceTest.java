/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.organizationgroup.domain.GroupMembershipType;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.organizationgroup.exception.SystemGroupModificationException;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.permission.event.UserPermissionsChangedEvent;
import com.emberone.user.domain.User;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AllMembersGroupServiceTest {

    @Mock OrganizationGroupRepository organizationGroupRepository;
    @Mock UserRepository userRepository;
    @Mock UserOrganizationGroupMembershipRepository membershipRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks AllMembersGroupService service;

    @Test
    void createsMemberWhenNoActiveMembershipExists() {
        OrganizationGroup group = allMembersGroup();
        User user = User.builder().id(1L).tenantId(7L).build();
        when(organizationGroupRepository.findByOrganizationIdAndType(
                        10L, OrganizationGroupType.ALL_MEMBERS))
                .thenReturn(Optional.of(group));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(membershipRepository.findByUser_IdAndOrganizationGroup_Id(1L, 20L))
                .thenReturn(List.of());

        service.ensureUserInAllMembersGroup(1L, 10L);

        ArgumentCaptor<UserOrganizationGroupMembership> captor =
                ArgumentCaptor.forClass(UserOrganizationGroupMembership.class);
        verify(membershipRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getMembershipType()).isEqualTo(GroupMembershipType.MEMBER);
        assertThat(captor.getValue().getStartDate()).isBeforeOrEqualTo(Instant.now());
        assertThat(captor.getValue().getStartDate()).isAfter(Instant.now().minusSeconds(5));
        assertThat(captor.getValue().getOrganizationGroup()).isSameAs(group);
        verify(eventPublisher).publishEvent(new UserPermissionsChangedEvent(7L, 1L));
    }

    @Test
    void activeOrFutureEndingMembershipMakesEnsureIdempotent() {
        OrganizationGroup group = allMembersGroup();
        User user = User.builder().id(1L).tenantId(7L).build();
        UserOrganizationGroupMembership existing =
                UserOrganizationGroupMembership.builder()
                        .startDate(Instant.now().minusSeconds(60))
                        .endDate(Instant.now().plusSeconds(3600))
                        .build();
        when(organizationGroupRepository.findByOrganizationIdAndType(
                        10L, OrganizationGroupType.ALL_MEMBERS))
                .thenReturn(Optional.of(group));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(membershipRepository.findByUser_IdAndOrganizationGroup_Id(1L, 20L))
                .thenReturn(List.of(existing));

        service.ensureUserInAllMembersGroup(1L, 10L);

        verify(membershipRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void missingGroupOrUserFailsWithoutCreatingMembership() {
        when(organizationGroupRepository.findByOrganizationIdAndType(
                        10L, OrganizationGroupType.ALL_MEMBERS))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.ensureUserInAllMembersGroup(1L, 10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");

        when(organizationGroupRepository.findByOrganizationIdAndType(
                        10L, OrganizationGroupType.ALL_MEMBERS))
                .thenReturn(Optional.of(allMembersGroup()));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.ensureUserInAllMembersGroup(1L, 10L))
                .isInstanceOf(UserNotFoundException.class);
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void onlyAllMembersGroupBlocksManualModification() {
        assertThat(service.isAllMembersGroup(allMembersGroup())).isTrue();
        assertThat(service.isAllMembersGroup(customGroup())).isFalse();
        assertThat(service.isLeadershipGroup(leadershipGroup())).isTrue();
        assertThat(service.isLeadershipGroup(customGroup())).isFalse();
        assertThatThrownBy(() -> service.validateMembershipModificationAllowed(allMembersGroup()))
                .isInstanceOf(SystemGroupModificationException.class);
        assertThatThrownBy(() -> service.validateMembershipModificationAllowed(leadershipGroup()))
                .isInstanceOf(SystemGroupModificationException.class);
        service.validateMembershipModificationAllowed(customGroup());
    }

    @Test
    void hasActiveLeadershipMembershipDelegatesToRepository() {
        when(membershipRepository.existsActiveLeadershipMembership(
                        eq(1L), eq(10L), any(Instant.class)))
                .thenReturn(true);

        assertThat(service.hasActiveLeadershipMembership(1L, 10L)).isTrue();
        verify(membershipRepository)
                .existsActiveLeadershipMembership(eq(1L), eq(10L), any(Instant.class));
    }

    private OrganizationGroup allMembersGroup() {
        return OrganizationGroup.builder()
                .id(20L)
                .name("Alle Mitglieder")
                .type(OrganizationGroupType.ALL_MEMBERS)
                .build();
    }

    private OrganizationGroup customGroup() {
        return OrganizationGroup.builder()
                .id(21L)
                .name("Crew")
                .type(OrganizationGroupType.CUSTOM)
                .build();
    }

    private OrganizationGroup leadershipGroup() {
        return OrganizationGroup.builder()
                .id(22L)
                .name("Führung")
                .type(OrganizationGroupType.LEADERSHIP)
                .build();
    }
}
