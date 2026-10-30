/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.organization.domain.Organization;
import com.emberone.organizationgroup.service.AllMembersGroupService;
import com.emberone.permission.event.UserPermissionsChangedEvent;
import com.emberone.user.domain.User;
import com.emberone.user.service.UserAccountContinuity;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.dto.UpdateUserOrganizationMembershipRequest;
import com.emberone.userorganizationmembership.dto.UserOrganizationMembershipDto;
import com.emberone.userorganizationmembership.event.OrganizationMembershipEndedEvent;
import com.emberone.userorganizationmembership.event.OrganizationMembershipUpdatedEvent;
import com.emberone.userorganizationmembership.exception.MembershipStartDateInFutureException;
import com.emberone.userorganizationmembership.exception.OrganizationMembershipAlreadyEndedException;
import com.emberone.userorganizationmembership.exception.OrganizationMembershipEndBlockedByLeadershipException;
import com.emberone.userorganizationmembership.mapper.UserOrganizationMembershipMapper;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class UserOrganizationMembershipServiceTest {

    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock UserOrganizationMembershipMapper mapper;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock AllMembersGroupService allMembersGroupService;
    @Mock UserAccountContinuity userAccountContinuity;

    @InjectMocks UserOrganizationMembershipService service;

    private static UserOrganizationMembership membership(
            Long id, Long userId, Long organizationId) {
        return UserOrganizationMembership.builder()
                .id(id)
                .tenantId(1L)
                .user(User.builder().id(userId).build())
                .organization(Organization.builder().id(organizationId).build())
                .startDate(Instant.now())
                .build();
    }

    @Test
    void updateMembershipUpdatesStartDateOnly() {
        UserOrganizationMembership membership = membership(5L, 10L, 20L);
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(membership));
        when(membershipRepository.findByUser_IdAndOrganization_Id(10L, 20L))
                .thenReturn(List.of(membership));
        when(membershipRepository.save(membership)).thenReturn(membership);
        when(mapper.toDto(membership))
                .thenReturn(UserOrganizationMembershipDto.builder().id(5L).build());

        Instant newStart = membership.getStartDate().minus(1, ChronoUnit.DAYS);
        UpdateUserOrganizationMembershipRequest request =
                UpdateUserOrganizationMembershipRequest.builder().startDate(newStart).build();

        service.updateMembership(5L, request);

        assertThat(membership.getStartDate()).isEqualTo(newStart);
        verify(membershipRepository).save(membership);
    }

    @Test
    void endMembershipWithFutureEndDateDoesNotApplyEndedEffectsYet() {
        UserOrganizationMembership membership = membership(5L, 10L, 20L);
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(membership));
        when(membershipRepository.save(membership)).thenReturn(membership);
        when(mapper.toDto(membership))
                .thenReturn(UserOrganizationMembershipDto.builder().id(5L).build());

        Instant endDate = membership.getStartDate().plus(1, ChronoUnit.DAYS);
        service.endMembership(5L, endDate);

        assertThat(membership.getEndProcessedAt()).isNull();
        verify(eventPublisher).publishEvent(new OrganizationMembershipUpdatedEvent(1L, 10L, 20L));
        verify(eventPublisher).publishEvent(new UserPermissionsChangedEvent(1L, 10L));
        verify(eventPublisher, never()).publishEvent(any(OrganizationMembershipEndedEvent.class));
    }

    @Test
    void endMembershipWithPastEndDateAppliesEndedEffects() {
        UserOrganizationMembership membership = membership(5L, 10L, 20L);
        membership.setStartDate(Instant.now().minus(10, ChronoUnit.DAYS));
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(membership));
        when(membershipRepository.save(membership)).thenReturn(membership);
        when(mapper.toDto(membership))
                .thenReturn(UserOrganizationMembershipDto.builder().id(5L).build());

        Instant endDate = Instant.now().minus(1, ChronoUnit.HOURS);
        service.endMembership(5L, endDate);

        assertThat(membership.getEndProcessedAt()).isNotNull();
        verify(eventPublisher)
                .publishEvent(
                        new OrganizationMembershipEndedEvent(
                                membership.getTenantId(), 10L, 20L, endDate));
        verify(eventPublisher)
                .publishEvent(new UserPermissionsChangedEvent(membership.getTenantId(), 10L));
    }

    @Test
    void processDueEndedMembershipsPublishesEndedEffectsAndMarksProcessed() {
        UserOrganizationMembership membership = membership(5L, 10L, 20L);
        Instant endDate = Instant.now().minus(1, ChronoUnit.HOURS);
        membership.setEndDate(endDate);
        when(membershipRepository.findDueEndedUnprocessed(any())).thenReturn(List.of(membership));
        when(membershipRepository.save(membership)).thenReturn(membership);

        service.processDueEndedMemberships();

        assertThat(membership.getEndProcessedAt()).isNotNull();
        verify(eventPublisher)
                .publishEvent(
                        new OrganizationMembershipEndedEvent(
                                membership.getTenantId(), 10L, 20L, endDate));
        verify(eventPublisher)
                .publishEvent(new UserPermissionsChangedEvent(membership.getTenantId(), 10L));
    }

    @Test
    void processDueEndedMembershipsDoesNothingWhenNoneDue() {
        when(membershipRepository.findDueEndedUnprocessed(any())).thenReturn(List.of());

        service.processDueEndedMemberships();

        verify(eventPublisher, never()).publishEvent(any());
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void endMembershipRejectsAlreadyEndedMembership() {
        UserOrganizationMembership membership = membership(5L, 10L, 20L);
        membership.setEndDate(Instant.now().minus(1, ChronoUnit.DAYS));
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> service.endMembership(5L, Instant.now()))
                .isInstanceOf(OrganizationMembershipAlreadyEndedException.class);
    }

    @Test
    void endMembershipRejectsWhenUserIsInLeadershipGroup() {
        UserOrganizationMembership membership = membership(5L, 10L, 20L);
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(membership));
        when(allMembersGroupService.hasActiveLeadershipMembership(10L, 20L)).thenReturn(true);

        assertThatThrownBy(() -> service.endMembership(5L, Instant.now()))
                .isInstanceOf(OrganizationMembershipEndBlockedByLeadershipException.class);

        verify(membershipRepository, never()).save(any());
    }

    @Test
    void updateMembershipRejectsWhenUserIsInLeadershipGroup() {
        UserOrganizationMembership membership = membership(5L, 10L, 20L);
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(membership));
        when(allMembersGroupService.hasActiveLeadershipMembership(10L, 20L)).thenReturn(true);

        UpdateUserOrganizationMembershipRequest request =
                UpdateUserOrganizationMembershipRequest.builder()
                        .startDate(membership.getStartDate())
                        .build();

        assertThatThrownBy(() -> service.updateMembership(5L, request))
                .isInstanceOf(OrganizationMembershipEndBlockedByLeadershipException.class);

        verify(membershipRepository, never()).save(any());
    }

    @Test
    void updateMembershipRejectsFutureStartDate() {
        UserOrganizationMembership membership = membership(5L, 10L, 20L);
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(membership));

        UpdateUserOrganizationMembershipRequest request =
                UpdateUserOrganizationMembershipRequest.builder()
                        .startDate(Instant.now().plus(1, ChronoUnit.DAYS))
                        .build();

        assertThatThrownBy(() -> service.updateMembership(5L, request))
                .isInstanceOf(MembershipStartDateInFutureException.class);

        verify(membershipRepository, never()).save(any());
    }

    @Test
    void changeMainOrganizationSwitchesExistingMain() {
        UserOrganizationMembership currentMain = membership(1L, 10L, 20L);
        currentMain.setMain(true);
        UserOrganizationMembership newMain = membership(2L, 10L, 30L);
        newMain.setMain(false);
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(10L), any(Instant.class)))
                .thenReturn(Optional.of(currentMain));
        when(membershipRepository.findActiveByUser_IdAndOrganization_Id(
                        eq(10L), eq(30L), any(Instant.class)))
                .thenReturn(Optional.of(newMain));
        when(membershipRepository.save(currentMain)).thenReturn(currentMain);
        when(membershipRepository.save(newMain)).thenReturn(newMain);

        service.changeMainOrganization(10L, 30L);

        assertThat(currentMain.getMain()).isFalse();
        assertThat(newMain.getMain()).isTrue();
        verify(eventPublisher).publishEvent(new OrganizationMembershipUpdatedEvent(1L, 10L, 20L));
        verify(eventPublisher).publishEvent(new OrganizationMembershipUpdatedEvent(1L, 10L, 30L));
        verify(eventPublisher).publishEvent(new UserPermissionsChangedEvent(1L, 10L));
    }

    @Test
    void changeMainOrganizationSetsMainWhenNoneExists() {
        UserOrganizationMembership newMain = membership(2L, 10L, 30L);
        newMain.setMain(false);
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(10L), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(membershipRepository.findActiveByUser_IdAndOrganization_Id(
                        eq(10L), eq(30L), any(Instant.class)))
                .thenReturn(Optional.of(newMain));
        when(membershipRepository.save(newMain)).thenReturn(newMain);

        service.changeMainOrganization(10L, 30L);

        assertThat(newMain.getMain()).isTrue();
        verify(eventPublisher).publishEvent(new OrganizationMembershipUpdatedEvent(1L, 10L, 30L));
        verify(eventPublisher).publishEvent(new UserPermissionsChangedEvent(1L, 10L));
    }
}
