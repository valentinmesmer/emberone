/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboarding.domain.OnboardingInvitationStatus;
import com.emberone.onboarding.dto.OnboardingStepsAvailableDto;
import com.emberone.onboarding.dto.SelfOnboardingInvitationDetailDto;
import com.emberone.onboarding.service.OnboardingInvitationProgressService;
import com.emberone.onboarding.service.OnboardingInvitationReadinessService;
import com.emberone.onboarding.service.OnboardingInvitationService;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.security.TokenService;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminOnboardingInvitationServiceTest {

    @Mock OrganizationRepository organizationRepository;
    @Mock OnboardingInvitationReadinessService onboardingInvitationReadinessService;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock OnboardingInvitationService onboardingInvitationService;
    @Mock OnboardingInvitationProgressService progressService;
    @Mock TokenService tokenService;

    @InjectMocks AdminOnboardingInvitationService service;

    @Test
    void getOnboardingAvailableTrueWhenStepsConfigured() {
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(onboardingInvitationReadinessService.hasConfiguredOnboardingSteps(organization))
                .thenReturn(true);

        OnboardingStepsAvailableDto result = service.getOnboardingAvailable(7L, 10L);

        assertThat(result.isAvailable()).isTrue();
    }

    @Test
    void getOnboardingAvailableFalseWhenNoSteps() {
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(onboardingInvitationReadinessService.hasConfiguredOnboardingSteps(organization))
                .thenReturn(false);

        assertThat(service.getOnboardingAvailable(7L, 10L).isAvailable()).isFalse();
    }

    @Test
    void getRecordingAvailableTrueWhenMemberWithoutInvitationAndStepsConfigured() {
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(20L), eq(10L), any(Instant.class)))
                .thenReturn(true);
        when(onboardingInvitationService.hasInvitationForUserAndOrganization(20L, 10L))
                .thenReturn(false);
        when(onboardingInvitationReadinessService.hasConfiguredOnboardingSteps(organization))
                .thenReturn(true);

        assertThat(service.getRecordingAvailable(7L, 10L, 20L).isAvailable()).isTrue();
    }

    @Test
    void getRecordingAvailableFalseWhenInvitationAlreadyExists() {
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(20L), eq(10L), any(Instant.class)))
                .thenReturn(true);
        when(onboardingInvitationService.hasInvitationForUserAndOrganization(20L, 10L))
                .thenReturn(true);

        assertThat(service.getRecordingAvailable(7L, 10L, 20L).isAvailable()).isFalse();
    }

    @Test
    void startRecordingCreatesInvitationForActiveMember() {
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(20L), eq(10L), any(Instant.class)))
                .thenReturn(true);
        when(onboardingInvitationReadinessService.hasConfiguredOnboardingSteps(organization))
                .thenReturn(true);
        when(tokenService.getReferencedUserId()).thenReturn(1L);
        OnboardingInvitation invitation =
                OnboardingInvitation.builder()
                        .id(3L)
                        .tenantId(7L)
                        .userId(20L)
                        .targetOrganizationId(10L)
                        .status(OnboardingInvitationStatus.OPEN)
                        .build();
        when(onboardingInvitationService.createForExistingUser(7L, 20L, 10L, 1L))
                .thenReturn(invitation);
        when(progressService.getDetailForOrganization(10L, 3L))
                .thenReturn(SelfOnboardingInvitationDetailDto.builder().id(3L).build());

        SelfOnboardingInvitationDetailDto detail = service.startRecording(7L, 10L, 20L);

        assertThat(detail.getId()).isEqualTo(3L);
        verify(onboardingInvitationService).createForExistingUser(7L, 20L, 10L, 1L);
    }

    @Test
    void startRecordingStoresNullInviterForPlatformAdmin() {
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(20L), eq(10L), any(Instant.class)))
                .thenReturn(true);
        when(onboardingInvitationReadinessService.hasConfiguredOnboardingSteps(organization))
                .thenReturn(true);
        when(tokenService.getReferencedUserId()).thenReturn(null);
        OnboardingInvitation invitation =
                OnboardingInvitation.builder()
                        .id(3L)
                        .tenantId(7L)
                        .userId(20L)
                        .targetOrganizationId(10L)
                        .status(OnboardingInvitationStatus.OPEN)
                        .build();
        when(onboardingInvitationService.createForExistingUser(7L, 20L, 10L, null))
                .thenReturn(invitation);
        when(progressService.getDetailForOrganization(10L, 3L))
                .thenReturn(SelfOnboardingInvitationDetailDto.builder().id(3L).build());

        service.startRecording(7L, 10L, 20L);

        verify(onboardingInvitationService).createForExistingUser(7L, 20L, 10L, null);
    }

    @Test
    void startRecordingRejectsNonMember() {
        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(Organization.builder().id(10L).tenantId(7L).build()));
        when(membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(20L), eq(10L), any(Instant.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> service.startRecording(7L, 10L, 20L))
                .isInstanceOf(ConflictException.class);
        verify(onboardingInvitationService, never())
                .createForExistingUser(any(), any(), any(), any());
    }

    @Test
    void completeDelegatesToRecordCompletedOnboarding() {
        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(Organization.builder().id(10L).tenantId(7L).build()));
        when(onboardingInvitationService.requireInvitationForOrganization(10L, 3L))
                .thenReturn(
                        OnboardingInvitation.builder()
                                .id(3L)
                                .tenantId(7L)
                                .userId(20L)
                                .targetOrganizationId(10L)
                                .status(OnboardingInvitationStatus.OPEN)
                                .build());

        when(tokenService.getReferencedUserId()).thenReturn(99L);

        service.complete(7L, 10L, 3L, "Member already completed paper forms");

        verify(onboardingInvitationService)
                .recordCompletedOnboarding(7L, 3L, 99L, "Member already completed paper forms");
    }

    @Test
    void getDetailForUserReturnsInvitationForOrganizationUser() {
        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(Organization.builder().id(10L).tenantId(7L).build()));
        when(onboardingInvitationService.requireInvitationForOrganizationUser(10L, 20L))
                .thenReturn(
                        OnboardingInvitation.builder()
                                .id(3L)
                                .tenantId(7L)
                                .userId(20L)
                                .targetOrganizationId(10L)
                                .status(OnboardingInvitationStatus.APPROVED)
                                .build());
        when(progressService.getDetailForOrganization(10L, 3L))
                .thenReturn(SelfOnboardingInvitationDetailDto.builder().id(3L).build());

        assertThat(service.getDetailForUser(7L, 10L, 20L).getId()).isEqualTo(3L);
    }
}
