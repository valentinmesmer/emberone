/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationgroupmembership.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.emberone.organization.domain.Organization;
import com.emberone.organizationgroup.domain.GroupMembershipType;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.organizationgroup.dto.OrganizationGroupMemberDto;
import com.emberone.organizationgroup.exception.OrganizationGroupNotFoundException;
import com.emberone.organizationgroup.exception.SystemGroupModificationException;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.organizationgroup.service.AllMembersGroupService;
import com.emberone.permission.event.UserPermissionsChangedEvent;
import com.emberone.user.domain.User;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.dto.CreateUserOrganizationGroupMembershipRequest;
import com.emberone.userorganizationgroupmembership.dto.UpdateUserOrganizationGroupMembershipRequest;
import com.emberone.userorganizationgroupmembership.dto.UserOrganizationGroupMembershipDto;
import com.emberone.userorganizationgroupmembership.event.OrganizationGroupMembershipActivatedEvent;
import com.emberone.userorganizationgroupmembership.event.OrganizationGroupMembershipDeactivatedEvent;
import com.emberone.userorganizationgroupmembership.exception.UserOrganizationGroupMembershipAlreadyExistsException;
import com.emberone.userorganizationgroupmembership.exception.UserOrganizationGroupMembershipNotFoundException;
import com.emberone.userorganizationgroupmembership.mapper.UserOrganizationGroupMembershipMapper;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationmembership.exception.MembershipStartDateInFutureException;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class UserOrganizationGroupMembershipServiceTest {

    @Mock UserOrganizationGroupMembershipRepository repository;
    @Mock UserRepository userRepository;
    @Mock UserOrganizationGroupMembershipMapper mapper;
    @Mock OrganizationGroupRepository groupRepository;
    @Mock UserOrganizationMembershipRepository userOrganizationMembershipRepository;
    @Mock AllMembersGroupService allMembersGroupService;
    @Mock ApplicationEventPublisher eventPublisher;

    private UserOrganizationGroupMembershipService service;

    @BeforeEach
    void setUp() {
        service =
                new UserOrganizationGroupMembershipService(
                        repository,
                        userRepository,
                        mapper,
                        groupRepository,
                        userOrganizationMembershipRepository,
                        allMembersGroupService,
                        eventPublisher);
        lenient()
                .when(
                        userOrganizationMembershipRepository
                                .existsActiveByUser_IdAndOrganization_Id(
                                        anyLong(), anyLong(), any(Instant.class)))
                .thenReturn(true);
    }

    @Test
    void createRejectsOverlappingDateRange() {
        OrganizationGroup group = group(false);
        CreateUserOrganizationGroupMembershipRequest request = request(GroupMembershipType.MEMBER);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(repository.findByUser_IdAndOrganizationGroup_Id(1L, 10L))
                .thenReturn(
                        List.of(
                                UserOrganizationGroupMembership.builder()
                                        .startDate(Instant.parse("2026-01-01T00:00:00Z"))
                                        .endDate(null)
                                        .build()));

        assertThatThrownBy(() -> service.createMembership(request))
                .isInstanceOf(UserOrganizationGroupMembershipAlreadyExistsException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void createAllowsFirstMemberWithoutLeader() {
        OrganizationGroup group = group(false);
        CreateUserOrganizationGroupMembershipRequest request = request(GroupMembershipType.MEMBER);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.createMembership(request);

        verify(repository).save(any(UserOrganizationGroupMembership.class));
        verify(eventPublisher).publishEvent(new OrganizationGroupMembershipActivatedEvent(1L, 10L));
        verify(eventPublisher).publishEvent(new UserPermissionsChangedEvent(7L, 1L));
    }

    @Test
    void createsActiveLeaderMembership() {
        OrganizationGroup group = group(false);
        CreateUserOrganizationGroupMembershipRequest request = request(GroupMembershipType.LEADER);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.createMembership(request);

        verify(repository).save(any(UserOrganizationGroupMembership.class));
        verify(eventPublisher).publishEvent(new OrganizationGroupMembershipActivatedEvent(1L, 10L));
    }

    @Test
    void createRejectsFutureStartDate() {
        OrganizationGroup group = group(false);
        CreateUserOrganizationGroupMembershipRequest request =
                CreateUserOrganizationGroupMembershipRequest.builder()
                        .userId(1L)
                        .organizationGroupId(10L)
                        .startDate(Instant.now().plus(5, ChronoUnit.DAYS))
                        .membershipType(GroupMembershipType.MEMBER)
                        .build();
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.createMembership(request))
                .isInstanceOf(MembershipStartDateInFutureException.class);
        verify(repository, never()).save(any());
        verify(eventPublisher, never())
                .publishEvent(any(OrganizationGroupMembershipActivatedEvent.class));
    }

    @Test
    void updateMembershipEndingTodayPublishesDeactivatedEvent() {
        OrganizationGroup group = group(false);
        User user = User.builder().id(1L).tenantId(7L).build();
        UserOrganizationGroupMembership membership =
                UserOrganizationGroupMembership.builder()
                        .id(5L)
                        .tenantId(7L)
                        .user(user)
                        .organizationGroup(group)
                        .membershipType(GroupMembershipType.MEMBER)
                        .startDate(Instant.now().minus(10, ChronoUnit.DAYS))
                        .endDate(null)
                        .build();
        UpdateUserOrganizationGroupMembershipRequest request =
                UpdateUserOrganizationGroupMembershipRequest.builder()
                        .startDate(Instant.now().minus(10, ChronoUnit.DAYS))
                        .endDate(Instant.now().minus(1, ChronoUnit.DAYS))
                        .membershipType(GroupMembershipType.MEMBER)
                        .build();
        when(repository.findById(5L)).thenReturn(Optional.of(membership));
        when(repository.findByUser_IdAndOrganizationGroup_Id(1L, 10L))
                .thenReturn(List.of(membership));
        when(repository.save(membership)).thenReturn(membership);
        when(mapper.toDto(membership))
                .thenReturn(UserOrganizationGroupMembershipDto.builder().id(5L).build());
        doAnswer(
                        invocation -> {
                            UpdateUserOrganizationGroupMembershipRequest req =
                                    invocation.getArgument(0);
                            UserOrganizationGroupMembership entity = invocation.getArgument(1);
                            entity.setStartDate(req.getStartDate());
                            entity.setEndDate(req.getEndDate());
                            entity.setMembershipType(req.getMembershipType());
                            return null;
                        })
                .when(mapper)
                .updateFromRequest(any(), any());

        service.updateMembership(5L, request);

        verify(eventPublisher)
                .publishEvent(new OrganizationGroupMembershipDeactivatedEvent(7L, 1L, 10L));
    }

    @Test
    void updateMembershipReactivatingPublishesActivatedEvent() {
        OrganizationGroup group = group(false);
        User user = User.builder().id(1L).tenantId(7L).build();
        UserOrganizationGroupMembership membership =
                UserOrganizationGroupMembership.builder()
                        .id(5L)
                        .user(user)
                        .organizationGroup(group)
                        .membershipType(GroupMembershipType.MEMBER)
                        .startDate(Instant.now().minus(30, ChronoUnit.DAYS))
                        .endDate(Instant.now().minus(5, ChronoUnit.DAYS))
                        .build();
        UpdateUserOrganizationGroupMembershipRequest request =
                UpdateUserOrganizationGroupMembershipRequest.builder()
                        .startDate(Instant.now().minus(30, ChronoUnit.DAYS))
                        .endDate(null)
                        .membershipType(GroupMembershipType.MEMBER)
                        .build();
        when(repository.findById(5L)).thenReturn(Optional.of(membership));
        when(repository.findByUser_IdAndOrganizationGroup_Id(1L, 10L))
                .thenReturn(List.of(membership));
        when(repository.save(membership)).thenReturn(membership);
        when(mapper.toDto(membership))
                .thenReturn(UserOrganizationGroupMembershipDto.builder().id(5L).build());
        doAnswer(
                        invocation -> {
                            UpdateUserOrganizationGroupMembershipRequest req =
                                    invocation.getArgument(0);
                            UserOrganizationGroupMembership entity = invocation.getArgument(1);
                            entity.setStartDate(req.getStartDate());
                            entity.setEndDate(req.getEndDate());
                            entity.setMembershipType(req.getMembershipType());
                            return null;
                        })
                .when(mapper)
                .updateFromRequest(any(), any());

        service.updateMembership(5L, request);

        verify(eventPublisher).publishEvent(new OrganizationGroupMembershipActivatedEvent(1L, 10L));
    }

    @Test
    void updateMembershipTypeClosesCurrentAndOpensNewMembership() {
        OrganizationGroup group = group(false);
        User user = User.builder().id(1L).tenantId(7L).build();
        UserOrganizationGroupMembership current =
                UserOrganizationGroupMembership.builder()
                        .id(5L)
                        .tenantId(7L)
                        .user(user)
                        .organizationGroup(group)
                        .membershipType(GroupMembershipType.LEADER)
                        .startDate(Instant.now().minus(2, ChronoUnit.DAYS))
                        .build();
        UpdateUserOrganizationGroupMembershipRequest request =
                UpdateUserOrganizationGroupMembershipRequest.builder()
                        .startDate(current.getStartDate())
                        .membershipType(GroupMembershipType.MEMBER)
                        .build();
        UserOrganizationGroupMembershipDto dto =
                UserOrganizationGroupMembershipDto.builder().id(6L).build();
        when(repository.findById(5L)).thenReturn(Optional.of(current));
        when(repository.save(any(UserOrganizationGroupMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDto(any(UserOrganizationGroupMembership.class))).thenReturn(dto);

        assertThat(service.updateMembership(5L, request)).isSameAs(dto);

        assertThat(current.getEndDate()).isNotNull();
        assertThat(current.getEndProcessedAt()).isEqualTo(current.getEndDate());
        assertThat(current.getMembershipType()).isEqualTo(GroupMembershipType.LEADER);
        var saved = org.mockito.ArgumentCaptor.forClass(UserOrganizationGroupMembership.class);
        verify(repository, org.mockito.Mockito.times(2)).save(saved.capture());
        UserOrganizationGroupMembership next = saved.getAllValues().get(1);
        assertThat(next.getMembershipType()).isEqualTo(GroupMembershipType.MEMBER);
        assertThat(next.getStartDate()).isEqualTo(current.getEndDate());
        assertThat(next.getEndDate()).isNull();
        verify(mapper, org.mockito.Mockito.never()).updateFromRequest(any(), any());
    }

    @Test
    void updateMembershipTypeOfEndedMembershipIsRejected() {
        User user = User.builder().id(1L).tenantId(7L).build();
        UserOrganizationGroupMembership ended =
                UserOrganizationGroupMembership.builder()
                        .id(5L)
                        .user(user)
                        .organizationGroup(group(false))
                        .membershipType(GroupMembershipType.LEADER)
                        .startDate(Instant.now().minus(5, ChronoUnit.DAYS))
                        .endDate(Instant.now().minus(1, ChronoUnit.DAYS))
                        .build();
        when(repository.findById(5L)).thenReturn(Optional.of(ended));

        assertThatThrownBy(
                        () ->
                                service.updateMembership(
                                        5L,
                                        UpdateUserOrganizationGroupMembershipRequest.builder()
                                                .startDate(ended.getStartDate())
                                                .membershipType(GroupMembershipType.MEMBER)
                                                .build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateMembershipPersistsNonOverlappingChange() {
        OrganizationGroup group = group(false);
        User user = User.builder().id(1L).tenantId(7L).build();
        UserOrganizationGroupMembership membership =
                UserOrganizationGroupMembership.builder()
                        .id(5L)
                        .user(user)
                        .organizationGroup(group)
                        .membershipType(GroupMembershipType.LEADER)
                        .startDate(Instant.now().minus(2, ChronoUnit.DAYS))
                        .build();
        UpdateUserOrganizationGroupMembershipRequest request =
                UpdateUserOrganizationGroupMembershipRequest.builder()
                        .startDate(Instant.now().minus(1, ChronoUnit.DAYS))
                        .membershipType(GroupMembershipType.LEADER)
                        .build();
        UserOrganizationGroupMembershipDto dto =
                UserOrganizationGroupMembershipDto.builder().id(5L).build();
        when(repository.findById(5L)).thenReturn(Optional.of(membership));
        when(repository.findByUser_IdAndOrganizationGroup_Id(1L, 10L))
                .thenReturn(List.of(membership));
        when(repository.save(membership)).thenReturn(membership);
        when(mapper.toDto(membership)).thenReturn(dto);

        assertThat(service.updateMembership(5L, request)).isSameAs(dto);

        verify(mapper).updateFromRequest(request, membership);
        verify(repository).save(membership);
    }

    @Test
    void updateRejectsInvalidDatesAndOverlappingMembership() {
        OrganizationGroup group = group(false);
        UserOrganizationGroupMembership membership =
                UserOrganizationGroupMembership.builder()
                        .id(5L)
                        .user(User.builder().id(1L).build())
                        .organizationGroup(group)
                        .membershipType(GroupMembershipType.MEMBER)
                        .startDate(Instant.now())
                        .build();
        when(repository.findById(5L)).thenReturn(Optional.of(membership));
        UpdateUserOrganizationGroupMembershipRequest invalid =
                UpdateUserOrganizationGroupMembershipRequest.builder()
                        .startDate(Instant.now())
                        .endDate(Instant.now().minus(1, ChronoUnit.DAYS))
                        .membershipType(GroupMembershipType.MEMBER)
                        .build();
        assertThatThrownBy(() -> service.updateMembership(5L, invalid))
                .isInstanceOf(IllegalArgumentException.class);

        UpdateUserOrganizationGroupMembershipRequest overlapping =
                UpdateUserOrganizationGroupMembershipRequest.builder()
                        .startDate(Instant.now())
                        .endDate(Instant.now().plus(2, ChronoUnit.DAYS))
                        .membershipType(GroupMembershipType.MEMBER)
                        .build();
        UserOrganizationGroupMembership other =
                UserOrganizationGroupMembership.builder()
                        .id(6L)
                        .startDate(Instant.now().minus(1, ChronoUnit.DAYS))
                        .endDate(null)
                        .build();
        when(repository.findByUser_IdAndOrganizationGroup_Id(1L, 10L))
                .thenReturn(List.of(membership, other));
        assertThatThrownBy(() -> service.updateMembership(5L, overlapping))
                .isInstanceOf(UserOrganizationGroupMembershipAlreadyExistsException.class);
    }

    @Test
    void createRejectsMissingUserOrGroup() {
        CreateUserOrganizationGroupMembershipRequest request = request(GroupMembershipType.LEADER);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createMembership(request))
                .isInstanceOf(UserNotFoundException.class);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));
        when(groupRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createMembership(request))
                .isInstanceOf(OrganizationGroupNotFoundException.class);
    }

    @Test
    void createRejectsUserNotBelongingToGroupOrganization() {
        OrganizationGroup group = group(false);
        CreateUserOrganizationGroupMembershipRequest request = request(GroupMembershipType.MEMBER);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(userOrganizationMembershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(20L), any(Instant.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> service.createMembership(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User does not belong to the specified organization");
        verify(repository, never()).save(any());
        verify(allMembersGroupService, never()).validateMembershipModificationAllowed(any());
    }

    @Test
    void createRejectsEndDateBeforeStartDate() {
        OrganizationGroup group = group(false);
        CreateUserOrganizationGroupMembershipRequest request =
                CreateUserOrganizationGroupMembershipRequest.builder()
                        .userId(1L)
                        .organizationGroupId(10L)
                        .startDate(Instant.now())
                        .endDate(Instant.now().minus(1, ChronoUnit.DAYS))
                        .membershipType(GroupMembershipType.LEADER)
                        .build();
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.createMembership(request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void allMembersGroupAllowsMembershipCreation() {
        OrganizationGroup group =
                OrganizationGroup.builder()
                        .id(10L)
                        .name("Alle Mitglieder")
                        .type(OrganizationGroupType.ALL_MEMBERS)
                        .organization(Organization.builder().id(20L).build())
                        .build();
        CreateUserOrganizationGroupMembershipRequest request = request(GroupMembershipType.MEMBER);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.createMembership(request);

        verify(repository).save(any(UserOrganizationGroupMembership.class));
    }

    @Test
    void createRejectsAllMembersModificationWhenServiceBlocks() {
        OrganizationGroup group = group(false);
        doThrow(new IllegalStateException("blocked"))
                .when(allMembersGroupService)
                .validateMembershipModificationAllowed(group);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.createMembership(request(GroupMembershipType.LEADER)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("blocked");
    }

    @Test
    void updateAllowsDemotingLastLeader() {
        OrganizationGroup group = group(false);
        UserOrganizationGroupMembership membership =
                UserOrganizationGroupMembership.builder()
                        .id(5L)
                        .user(User.builder().id(1L).tenantId(7L).build())
                        .organizationGroup(group)
                        .membershipType(GroupMembershipType.LEADER)
                        .startDate(Instant.now().minus(1, ChronoUnit.DAYS))
                        .build();
        UpdateUserOrganizationGroupMembershipRequest request =
                UpdateUserOrganizationGroupMembershipRequest.builder()
                        .startDate(Instant.now().minus(1, ChronoUnit.DAYS))
                        .membershipType(GroupMembershipType.MEMBER)
                        .build();
        when(repository.findById(5L)).thenReturn(Optional.of(membership));
        when(repository.save(any(UserOrganizationGroupMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDto(any(UserOrganizationGroupMembership.class)))
                .thenReturn(UserOrganizationGroupMembershipDto.builder().id(5L).build());

        service.updateMembership(5L, request);

        verify(repository, org.mockito.Mockito.times(2))
                .save(any(UserOrganizationGroupMembership.class));
        assertThat(membership.getEndDate()).isNotNull();
    }

    @Test
    void createRejectsFutureLeaderStartDate() {
        OrganizationGroup group = group(false);
        CreateUserOrganizationGroupMembershipRequest request =
                CreateUserOrganizationGroupMembershipRequest.builder()
                        .userId(1L)
                        .organizationGroupId(10L)
                        .startDate(Instant.now().plus(7, ChronoUnit.DAYS))
                        .membershipType(GroupMembershipType.LEADER)
                        .build();
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.createMembership(request))
                .isInstanceOf(MembershipStartDateInFutureException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void createAllowsNonOverlappingBoundedMembership() {
        OrganizationGroup group = group(false);
        CreateUserOrganizationGroupMembershipRequest request =
                CreateUserOrganizationGroupMembershipRequest.builder()
                        .userId(1L)
                        .organizationGroupId(10L)
                        .startDate(Instant.parse("2026-07-01T00:00:00Z"))
                        .endDate(Instant.parse("2026-12-31T00:00:00Z"))
                        .membershipType(GroupMembershipType.MEMBER)
                        .build();
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(repository.findByUser_IdAndOrganizationGroup_Id(1L, 10L))
                .thenReturn(
                        List.of(
                                UserOrganizationGroupMembership.builder()
                                        .startDate(Instant.parse("2026-01-01T00:00:00Z"))
                                        .endDate(Instant.parse("2026-06-30T00:00:00Z"))
                                        .build()));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.createMembership(request);

        verify(repository).save(any(UserOrganizationGroupMembership.class));
    }

    @Test
    void updatePromotesMemberToLeader() {
        OrganizationGroup group = group(false);
        User user = User.builder().id(1L).tenantId(7L).build();
        UserOrganizationGroupMembership membership =
                UserOrganizationGroupMembership.builder()
                        .id(5L)
                        .user(user)
                        .organizationGroup(group)
                        .membershipType(GroupMembershipType.MEMBER)
                        .startDate(Instant.now().minus(1, ChronoUnit.DAYS))
                        .build();
        UpdateUserOrganizationGroupMembershipRequest request =
                UpdateUserOrganizationGroupMembershipRequest.builder()
                        .startDate(Instant.now().minus(1, ChronoUnit.DAYS))
                        .membershipType(GroupMembershipType.LEADER)
                        .build();
        when(repository.findById(5L)).thenReturn(Optional.of(membership));
        when(repository.save(any(UserOrganizationGroupMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDto(any(UserOrganizationGroupMembership.class)))
                .thenReturn(UserOrganizationGroupMembershipDto.builder().id(5L).build());

        service.updateMembership(5L, request);

        verify(repository, org.mockito.Mockito.times(2))
                .save(any(UserOrganizationGroupMembership.class));
        assertThat(membership.getEndDate()).isNotNull();
    }

    @Test
    void updateRejectsFutureStartDate() {
        OrganizationGroup group = group(false);
        UserOrganizationGroupMembership membership =
                UserOrganizationGroupMembership.builder()
                        .id(5L)
                        .user(User.builder().id(1L).tenantId(7L).build())
                        .organizationGroup(group)
                        .membershipType(GroupMembershipType.LEADER)
                        .startDate(Instant.now().minus(1, ChronoUnit.DAYS))
                        .build();
        UpdateUserOrganizationGroupMembershipRequest request =
                UpdateUserOrganizationGroupMembershipRequest.builder()
                        .startDate(Instant.now().plus(7, ChronoUnit.DAYS))
                        .membershipType(GroupMembershipType.LEADER)
                        .build();
        when(repository.findById(5L)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> service.updateMembership(5L, request))
                .isInstanceOf(MembershipStartDateInFutureException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void updateRejectsMissingMembership() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.updateMembership(
                                        5L,
                                        UpdateUserOrganizationGroupMembershipRequest.builder()
                                                .startDate(Instant.now())
                                                .membershipType(GroupMembershipType.MEMBER)
                                                .build()))
                .isInstanceOf(UserOrganizationGroupMembershipNotFoundException.class);
    }

    @Test
    void endAllActiveMembershipsForUserInOrganizationSetsEndDateOnActiveMemberships() {
        OrganizationGroup group = group(false);
        UserOrganizationGroupMembership membership =
                UserOrganizationGroupMembership.builder()
                        .id(5L)
                        .tenantId(7L)
                        .organizationGroup(group)
                        .user(User.builder().id(1L).build())
                        .membershipType(GroupMembershipType.MEMBER)
                        .startDate(Instant.now().minus(10, ChronoUnit.DAYS))
                        .build();
        Instant endDate = Instant.parse("2026-06-13T00:00:00Z");
        when(repository.findActiveByUser_IdAndOrganization_Id(1L, 20L, endDate))
                .thenReturn(List.of(membership));

        service.endAllActiveMembershipsForUserInOrganization(1L, 20L, endDate);

        assertThat(membership.getEndDate()).isEqualTo(endDate);
        assertThat(membership.getEndProcessedAt()).isNotNull();
        verify(repository).save(membership);
        verify(eventPublisher).publishEvent(new UserPermissionsChangedEvent(7L, 1L));
    }

    @Test
    void endActiveMembershipSetsEndDateAndPublishesDeactivated() {
        OrganizationGroup group = group(false);
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        UserOrganizationGroupMembership membership =
                UserOrganizationGroupMembership.builder()
                        .id(5L)
                        .tenantId(7L)
                        .organizationGroup(group)
                        .user(User.builder().id(1L).build())
                        .membershipType(GroupMembershipType.MEMBER)
                        .startDate(start)
                        .build();
        Instant endDate = Instant.parse("2026-06-13T00:00:00Z");
        when(repository.findActiveByUser_IdAndOrganizationGroup_Id(
                        eq(1L), eq(10L), any(Instant.class)))
                .thenReturn(Optional.of(membership));

        service.endActiveMembership(1L, 10L, endDate);

        assertThat(membership.getEndDate()).isEqualTo(endDate);
        assertThat(membership.getEndProcessedAt()).isNotNull();
        verify(repository).save(membership);
        verify(eventPublisher)
                .publishEvent(new OrganizationGroupMembershipDeactivatedEvent(7L, 1L, 10L));
        verify(eventPublisher).publishEvent(new UserPermissionsChangedEvent(7L, 1L));
    }

    @Test
    void endActiveMembershipRejectsAllMembersGroup() {
        OrganizationGroup group =
                OrganizationGroup.builder()
                        .id(10L)
                        .name("All")
                        .type(OrganizationGroupType.ALL_MEMBERS)
                        .organization(Organization.builder().id(20L).build())
                        .build();
        UserOrganizationGroupMembership membership =
                UserOrganizationGroupMembership.builder()
                        .id(5L)
                        .tenantId(7L)
                        .organizationGroup(group)
                        .user(User.builder().id(1L).build())
                        .membershipType(GroupMembershipType.MEMBER)
                        .startDate(Instant.now().minus(10, ChronoUnit.DAYS))
                        .build();
        when(repository.findActiveByUser_IdAndOrganizationGroup_Id(
                        eq(1L), eq(10L), any(Instant.class)))
                .thenReturn(Optional.of(membership));
        doThrow(
                        new SystemGroupModificationException(
                                "Cannot manually modify memberships in the AllMembers organization group"))
                .when(allMembersGroupService)
                .validateMembershipModificationAllowed(group);

        assertThatThrownBy(() -> service.endActiveMembership(1L, 10L, Instant.now()))
                .isInstanceOf(SystemGroupModificationException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void endActiveMembershipRejectsLeadershipGroup() {
        OrganizationGroup group =
                OrganizationGroup.builder()
                        .id(10L)
                        .name("Führung")
                        .type(OrganizationGroupType.LEADERSHIP)
                        .organization(Organization.builder().id(20L).build())
                        .build();
        UserOrganizationGroupMembership membership =
                UserOrganizationGroupMembership.builder()
                        .id(5L)
                        .tenantId(7L)
                        .organizationGroup(group)
                        .user(User.builder().id(1L).build())
                        .membershipType(GroupMembershipType.MEMBER)
                        .startDate(Instant.now().minus(10, ChronoUnit.DAYS))
                        .build();
        when(repository.findActiveByUser_IdAndOrganizationGroup_Id(
                        eq(1L), eq(10L), any(Instant.class)))
                .thenReturn(Optional.of(membership));
        doThrow(
                        new SystemGroupModificationException(
                                "Cannot manually modify memberships in the Leadership organization group"))
                .when(allMembersGroupService)
                .validateMembershipModificationAllowed(group);

        assertThatThrownBy(() -> service.endActiveMembership(1L, 10L, Instant.now()))
                .isInstanceOf(SystemGroupModificationException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void processDueEndedMembershipsPublishesDeactivatedEffects() {
        OrganizationGroup group = group(false);
        Instant endDate = Instant.now().minus(1, ChronoUnit.MINUTES);
        UserOrganizationGroupMembership membership =
                UserOrganizationGroupMembership.builder()
                        .id(5L)
                        .tenantId(7L)
                        .organizationGroup(group)
                        .user(User.builder().id(1L).build())
                        .membershipType(GroupMembershipType.MEMBER)
                        .startDate(Instant.now().minus(10, ChronoUnit.DAYS))
                        .endDate(endDate)
                        .build();
        when(repository.findDueEndedUnprocessed(any())).thenReturn(List.of(membership));
        when(repository.save(membership)).thenReturn(membership);

        service.processDueEndedMemberships();

        assertThat(membership.getEndProcessedAt()).isNotNull();
        verify(eventPublisher)
                .publishEvent(new OrganizationGroupMembershipDeactivatedEvent(7L, 1L, 10L));
        verify(eventPublisher).publishEvent(new UserPermissionsChangedEvent(7L, 1L));
    }

    @Test
    void processDueEndedMembershipsDoesNothingWhenNoneDue() {
        when(repository.findDueEndedUnprocessed(any())).thenReturn(List.of());

        service.processDueEndedMemberships();

        verify(eventPublisher, never()).publishEvent(any());
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsBoundedOverlappingMembership() {
        OrganizationGroup group = group(false);
        CreateUserOrganizationGroupMembershipRequest request =
                CreateUserOrganizationGroupMembershipRequest.builder()
                        .userId(1L)
                        .organizationGroupId(10L)
                        .startDate(Instant.parse("2026-04-01T00:00:00Z"))
                        .endDate(Instant.parse("2026-09-30T00:00:00Z"))
                        .membershipType(GroupMembershipType.MEMBER)
                        .build();
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(repository.findByUser_IdAndOrganizationGroup_Id(1L, 10L))
                .thenReturn(
                        List.of(
                                UserOrganizationGroupMembership.builder()
                                        .startDate(Instant.parse("2026-01-01T00:00:00Z"))
                                        .endDate(Instant.parse("2026-06-30T00:00:00Z"))
                                        .build()));

        assertThatThrownBy(() -> service.createMembership(request))
                .isInstanceOf(UserOrganizationGroupMembershipAlreadyExistsException.class);
    }

    @Test
    void createRejectsMembershipWhenRangesOverlap() {
        OrganizationGroup group = group(false);
        Instant boundary = Instant.parse("2026-07-01T00:00:00Z");
        CreateUserOrganizationGroupMembershipRequest request =
                CreateUserOrganizationGroupMembershipRequest.builder()
                        .userId(1L)
                        .organizationGroupId(10L)
                        .startDate(boundary)
                        .endDate(null)
                        .membershipType(GroupMembershipType.MEMBER)
                        .build();
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(repository.findByUser_IdAndOrganizationGroup_Id(1L, 10L))
                .thenReturn(
                        List.of(
                                UserOrganizationGroupMembership.builder()
                                        .startDate(Instant.parse("2026-01-01T00:00:00Z"))
                                        .endDate(boundary.plusMillis(1))
                                        .build()));

        assertThatThrownBy(() -> service.createMembership(request))
                .isInstanceOf(UserOrganizationGroupMembershipAlreadyExistsException.class);
    }

    @Test
    void updateAllowsMemberToMemberDateChange() {
        OrganizationGroup group = group(false);
        User user = User.builder().id(1L).tenantId(7L).build();
        UserOrganizationGroupMembership membership =
                UserOrganizationGroupMembership.builder()
                        .id(5L)
                        .user(user)
                        .organizationGroup(group)
                        .membershipType(GroupMembershipType.MEMBER)
                        .startDate(Instant.now().minus(10, ChronoUnit.DAYS))
                        .endDate(Instant.now().plus(10, ChronoUnit.DAYS))
                        .build();
        UpdateUserOrganizationGroupMembershipRequest request =
                UpdateUserOrganizationGroupMembershipRequest.builder()
                        .startDate(Instant.now().minus(5, ChronoUnit.DAYS))
                        .endDate(Instant.now().plus(20, ChronoUnit.DAYS))
                        .membershipType(GroupMembershipType.MEMBER)
                        .build();
        when(repository.findById(5L)).thenReturn(Optional.of(membership));
        when(repository.findByUser_IdAndOrganizationGroup_Id(1L, 10L))
                .thenReturn(List.of(membership));
        when(repository.save(membership)).thenReturn(membership);
        when(mapper.toDto(membership))
                .thenReturn(UserOrganizationGroupMembershipDto.builder().id(5L).build());

        service.updateMembership(5L, request);

        verify(repository).save(membership);
    }

    @Test
    void updateRejectsOverlappingMembershipExcludingCurrent() {
        OrganizationGroup group = group(false);
        UserOrganizationGroupMembership membership =
                UserOrganizationGroupMembership.builder()
                        .id(5L)
                        .user(User.builder().id(1L).build())
                        .organizationGroup(group)
                        .membershipType(GroupMembershipType.MEMBER)
                        .startDate(Instant.parse("2026-01-01T00:00:00Z"))
                        .endDate(Instant.parse("2026-03-31T00:00:00Z"))
                        .build();
        UserOrganizationGroupMembership other =
                UserOrganizationGroupMembership.builder()
                        .id(6L)
                        .startDate(Instant.parse("2026-05-01T00:00:00Z"))
                        .endDate(Instant.parse("2026-08-31T00:00:00Z"))
                        .build();
        UpdateUserOrganizationGroupMembershipRequest request =
                UpdateUserOrganizationGroupMembershipRequest.builder()
                        .startDate(Instant.parse("2026-04-01T00:00:00Z"))
                        .endDate(Instant.parse("2026-06-30T00:00:00Z"))
                        .membershipType(GroupMembershipType.MEMBER)
                        .build();
        when(repository.findById(5L)).thenReturn(Optional.of(membership));
        when(repository.findByUser_IdAndOrganizationGroup_Id(1L, 10L))
                .thenReturn(List.of(membership, other));

        assertThatThrownBy(() -> service.updateMembership(5L, request))
                .isInstanceOf(UserOrganizationGroupMembershipAlreadyExistsException.class);
    }

    @Test
    void membersAreMappedUniquelyAndSortedByName() {
        OrganizationGroup group = group(false);
        User ada = User.builder().id(2L).firstName("Ada").lastName("Zulu").build();
        User bob = User.builder().id(1L).firstName("Bob").lastName("Alpha").build();
        Instant now = Instant.now();
        when(repository.findActiveByOrganizationGroup_IdWithUser(eq(10L), any(Instant.class)))
                .thenReturn(
                        List.of(
                                UserOrganizationGroupMembership.builder()
                                        .user(ada)
                                        .organizationGroup(group)
                                        .startDate(now.minus(1, ChronoUnit.DAYS))
                                        .build(),
                                UserOrganizationGroupMembership.builder()
                                        .user(bob)
                                        .organizationGroup(group)
                                        .startDate(now.minus(1, ChronoUnit.DAYS))
                                        .build()));

        assertThat(service.getMembersByOrganizationGroupId(10L))
                .extracting(OrganizationGroupMemberDto::getId)
                .containsExactly(1L, 2L);
    }

    @Test
    void getMembersByOrganizationGroupIdReturnsOnlyActiveMemberships() {
        OrganizationGroup group = group(false);
        User activeUser = User.builder().id(1L).firstName("Active").lastName("User").build();
        Instant now = Instant.now();
        when(repository.findActiveByOrganizationGroup_IdWithUser(eq(10L), any(Instant.class)))
                .thenReturn(
                        List.of(
                                UserOrganizationGroupMembership.builder()
                                        .user(activeUser)
                                        .organizationGroup(group)
                                        .startDate(now.minus(30, ChronoUnit.DAYS))
                                        .endDate(null)
                                        .build()));

        assertThat(service.getMembersByOrganizationGroupId(10L))
                .extracting(OrganizationGroupMemberDto::getId)
                .containsExactly(1L);

        verify(repository, never()).findByOrganizationGroup_IdWithUser(any());
    }

    @Test
    void createRejectsLeadershipGroupModification() {
        OrganizationGroup group = group(true);
        CreateUserOrganizationGroupMembershipRequest request = request(GroupMembershipType.LEADER);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        doThrow(new SystemGroupModificationException("blocked"))
                .when(allMembersGroupService)
                .validateMembershipModificationAllowed(group);

        assertThatThrownBy(() -> service.createMembership(request))
                .isInstanceOf(SystemGroupModificationException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void createSystemManagedMembershipSkipsValidation() {
        OrganizationGroup group = group(true);
        CreateUserOrganizationGroupMembershipRequest request = request(GroupMembershipType.LEADER);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.createSystemManagedMembership(request);

        verify(allMembersGroupService, never()).validateMembershipModificationAllowed(any());
        verify(repository).save(any(UserOrganizationGroupMembership.class));
    }

    @Test
    void updateSystemManagedMembershipSkipsValidation() {
        OrganizationGroup group = group(true);
        User user = User.builder().id(1L).tenantId(7L).build();
        UserOrganizationGroupMembership membership =
                UserOrganizationGroupMembership.builder()
                        .id(5L)
                        .user(user)
                        .organizationGroup(group)
                        .membershipType(GroupMembershipType.LEADER)
                        .startDate(Instant.now().minus(2, ChronoUnit.DAYS))
                        .build();
        UpdateUserOrganizationGroupMembershipRequest request =
                UpdateUserOrganizationGroupMembershipRequest.builder()
                        .startDate(Instant.now().minus(1, ChronoUnit.DAYS))
                        .membershipType(GroupMembershipType.MEMBER)
                        .build();
        when(repository.findById(5L)).thenReturn(Optional.of(membership));
        when(repository.save(any(UserOrganizationGroupMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDto(any(UserOrganizationGroupMembership.class)))
                .thenReturn(UserOrganizationGroupMembershipDto.builder().id(5L).build());

        service.updateSystemManagedMembership(5L, request);

        verify(allMembersGroupService, never()).validateMembershipModificationAllowed(any());
        verify(repository, org.mockito.Mockito.times(2))
                .save(any(UserOrganizationGroupMembership.class));
        assertThat(membership.getEndDate()).isNotNull();
    }

    private OrganizationGroup group(boolean leadership) {
        return OrganizationGroup.builder()
                .id(10L)
                .name("Crew")
                .type(leadership ? OrganizationGroupType.LEADERSHIP : OrganizationGroupType.CUSTOM)
                .organization(Organization.builder().id(20L).build())
                .build();
    }

    private CreateUserOrganizationGroupMembershipRequest request(GroupMembershipType type) {
        return CreateUserOrganizationGroupMembershipRequest.builder()
                .userId(1L)
                .organizationGroupId(10L)
                .startDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .membershipType(type)
                .build();
    }
}
