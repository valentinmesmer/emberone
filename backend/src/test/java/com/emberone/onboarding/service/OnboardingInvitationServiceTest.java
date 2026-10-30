/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboarding.domain.OnboardingInvitationStatus;
import com.emberone.onboarding.dto.OnboardingInvitationPageDto;
import com.emberone.onboarding.repository.OnboardingInvitationRepository;
import com.emberone.onboarding.repository.OnboardingInvitationStepDataRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.user.domain.User;
import com.emberone.user.domain.UserContactInformation;
import com.emberone.user.repository.UserRepository;
import com.emberone.userdocument.service.UserDocumentService;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class OnboardingInvitationServiceTest {

    @Mock OrganizationRepository organizationRepository;
    @Mock OnboardingInvitationRepository onboardingInvitationRepository;
    @Mock OnboardingInvitationStepDataRepository stepDataRepository;
    @Mock UserRepository userRepository;
    @Mock UserOrganizationMembershipRepository userOrganizationMembershipRepository;
    @Mock UserDocumentService userDocumentService;
    @Mock OnboardingPromotionService onboardingPromotionService;
    @Mock OnboardingInvitationReadinessService onboardingInvitationReadinessService;
    @Mock OnboardingInvitationDraftStorageService invitationDraftStorageService;

    @InjectMocks OnboardingInvitationService service;

    @Test
    void listForOrganizationMapsPageContent() {
        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(Organization.builder().id(10L).tenantId(7L).build()));
        Instant invitedAt = Instant.parse("2026-08-31T10:00:00Z");
        OnboardingInvitation laterInvite = invitation(2L, 20L, OnboardingInvitationStatus.OPEN);
        laterInvite.setInvitedAt(invitedAt);
        laterInvite.setUser(user(20L, "Zed", "Zebra", 8L, "zed@example.com"));
        OnboardingInvitation earlierName =
                invitation(1L, 21L, OnboardingInvitationStatus.SUBMITTED);
        earlierName.setInvitedAt(invitedAt);
        earlierName.setUser(user(21L, "Ada", "Lovelace", 3L, "ada@example.com"));
        when(onboardingInvitationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(earlierName, laterInvite)));

        OnboardingInvitationPageDto result =
                service.listForOrganization(10L, new ListQuery(0, null, null, null));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getLastName()).isEqualTo("Lovelace");
        assertThat(result.getContent().get(0).getUserId()).isEqualTo(21L);
        assertThat(result.getContent().get(0).getPersonalNumber()).isEqualTo(3L);
        assertThat(result.getContent().get(0).getPrivateEmail()).isEqualTo("ada@example.com");
        assertThat(result.getContent().get(0).getStatus())
                .isEqualTo(OnboardingInvitationStatus.SUBMITTED);
        assertThat(result.getContent().get(1).getLastName()).isEqualTo("Zebra");
        assertThat(result.getContent().get(1).getInvitedAt()).isEqualTo(invitedAt);
    }

    @Test
    void listForOrganizationSkipsInvitationsWithoutUser() {
        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(Organization.builder().id(10L).tenantId(7L).build()));
        when(onboardingInvitationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(
                        new PageImpl<>(
                                List.of(invitation(1L, 99L, OnboardingInvitationStatus.OPEN))));

        OnboardingInvitationPageDto result =
                service.listForOrganization(10L, new ListQuery(0, null, null, null));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void listForOrganizationRejectsUnknownOrganization() {
        when(organizationRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> service.listForOrganization(10L, new ListQuery(0, null, null, null)))
                .isInstanceOf(OrganizationNotFoundException.class);
    }

    @Test
    void deletePendingForUserCleansStoredUploadsAndDeletesPendingInvitations() {
        OnboardingInvitation invitation = invitation(5L, 20L, OnboardingInvitationStatus.OPEN);
        when(onboardingInvitationRepository.findByUserIdAndStatusIn(
                        20L, OnboardingInvitationStatus.pending()))
                .thenReturn(List.of(invitation));
        service.deletePendingForUser(20L);

        verify(userDocumentService)
                .deleteDocumentsForLinkTargets(7L, AppLinkType.ONBOARDING_INVITATION, List.of(5L));
        verify(invitationDraftStorageService).deleteDraftFilesForInvitation(5L);
        verify(onboardingInvitationRepository).delete(invitation);
    }

    @Test
    void hasPendingForUserAndOrganizationDelegatesToRepository() {
        when(onboardingInvitationRepository.existsByUserIdAndTargetOrganizationIdAndStatusIn(
                        20L, 10L, OnboardingInvitationStatus.pending()))
                .thenReturn(true);

        assertThat(service.hasPendingForUserAndOrganization(20L, 10L)).isTrue();
    }

    @Test
    void createForExistingUserSavesOpenInvitation() {
        when(onboardingInvitationRepository.findByUserIdAndTargetOrganizationId(20L, 10L))
                .thenReturn(Optional.empty());
        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(Organization.builder().id(10L).tenantId(7L).build()));
        when(userRepository.findById(20L))
                .thenReturn(Optional.of(user(20L, "Ada", "Lovelace", 3L, "ada@example.com")));
        OnboardingInvitation saved = invitation(9L, 20L, OnboardingInvitationStatus.OPEN);
        when(onboardingInvitationRepository.save(any())).thenReturn(saved);

        OnboardingInvitation result = service.createForExistingUser(7L, 20L, 10L, 99L);

        assertThat(result.getId()).isEqualTo(9L);
        ArgumentCaptor<OnboardingInvitation> captor =
                ArgumentCaptor.forClass(OnboardingInvitation.class);
        verify(onboardingInvitationRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getUserId()).isEqualTo(20L);
        assertThat(captor.getValue().getTargetOrganizationId()).isEqualTo(10L);
        assertThat(captor.getValue().getStatus()).isEqualTo(OnboardingInvitationStatus.OPEN);
        assertThat(captor.getValue().getInvitedByUserId()).isEqualTo(99L);
    }

    @Test
    void createSucceededEmptyForExistingUserSavesApprovedInvitation() {
        when(onboardingInvitationRepository.findByUserIdAndTargetOrganizationId(20L, 10L))
                .thenReturn(Optional.empty());
        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(Organization.builder().id(10L).tenantId(7L).build()));
        when(userRepository.findById(20L))
                .thenReturn(Optional.of(user(20L, "Ada", "Lovelace", 3L, "ada@example.com")));
        OnboardingInvitation saved = invitation(9L, 20L, OnboardingInvitationStatus.APPROVED);
        when(onboardingInvitationRepository.save(any())).thenReturn(saved);

        OnboardingInvitation result =
                service.createSucceededEmptyForExistingUser(7L, 20L, 10L, null);

        assertThat(result.getId()).isEqualTo(9L);
        ArgumentCaptor<OnboardingInvitation> captor =
                ArgumentCaptor.forClass(OnboardingInvitation.class);
        verify(onboardingInvitationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OnboardingInvitationStatus.APPROVED);
        assertThat(captor.getValue().getInvitedByUserId()).isNull();
    }

    @Test
    void recordInvitationForAdmissionOpensWhenStepsNotLiveFulfilled() {
        when(onboardingInvitationReadinessService.areAllStepsLiveFulfilled(7L, 20L, 10L))
                .thenReturn(false);
        when(onboardingInvitationRepository.findByUserIdAndTargetOrganizationId(20L, 10L))
                .thenReturn(Optional.empty());
        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(Organization.builder().id(10L).tenantId(7L).build()));
        when(userRepository.findById(20L))
                .thenReturn(Optional.of(user(20L, "Ada", "Lovelace", 3L, "ada@example.com")));
        when(onboardingInvitationRepository.save(any()))
                .thenReturn(invitation(9L, 20L, OnboardingInvitationStatus.OPEN));

        assertThat(service.recordInvitationForAdmission(7L, 20L, 10L, 99L)).isFalse();
    }

    @Test
    void recordInvitationForAdmissionSucceedsEmptyWhenLiveFulfilled() {
        when(onboardingInvitationReadinessService.areAllStepsLiveFulfilled(7L, 20L, 10L))
                .thenReturn(true);
        when(onboardingInvitationRepository.findByUserIdAndTargetOrganizationId(20L, 10L))
                .thenReturn(Optional.empty());
        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(Organization.builder().id(10L).tenantId(7L).build()));
        when(userRepository.findById(20L))
                .thenReturn(Optional.of(user(20L, "Ada", "Lovelace", 3L, "ada@example.com")));
        when(onboardingInvitationRepository.save(any()))
                .thenReturn(invitation(9L, 20L, OnboardingInvitationStatus.APPROVED));

        assertThat(service.recordInvitationForAdmission(7L, 20L, 10L, null)).isTrue();
    }

    @Test
    void createForExistingUserRejectsExistingInvitation() {
        when(onboardingInvitationRepository.findByUserIdAndTargetOrganizationId(20L, 10L))
                .thenReturn(Optional.of(invitation(5L, 20L, OnboardingInvitationStatus.OPEN)));

        assertThatThrownBy(() -> service.createForExistingUser(7L, 20L, 10L, null))
                .isInstanceOf(ConflictException.class);
        verify(onboardingInvitationRepository, never()).save(any());
    }

    @Test
    void recordCompletedOnboardingPromotesAndApprovesOpenInvitation() {
        OnboardingInvitation invitation = invitation(5L, 20L, OnboardingInvitationStatus.OPEN);
        when(onboardingInvitationRepository.findById(5L)).thenReturn(Optional.of(invitation));
        when(userOrganizationMembershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(20L), eq(10L), any(Instant.class)))
                .thenReturn(true);
        when(onboardingInvitationReadinessService.areAllStepsFulfilled(invitation))
                .thenReturn(true);

        service.recordCompletedOnboarding(7L, 5L, 99L, "Recorded during admin intake");

        verify(onboardingPromotionService)
                .promoteAllSteps(
                        invitation,
                        99L,
                        com.emberone.onboardingdocument.domain.EntryDocumentFulfillmentChannel
                                .ADMIN_RECORDED,
                        "Recorded during admin intake");
        assertThat(invitation.getStatus()).isEqualTo(OnboardingInvitationStatus.APPROVED);
        verify(onboardingInvitationRepository).save(invitation);
    }

    @Test
    void recordCompletedOnboardingRejectsWhenNotOpen() {
        when(onboardingInvitationRepository.findById(5L))
                .thenReturn(Optional.of(invitation(5L, 20L, OnboardingInvitationStatus.SUBMITTED)));

        assertThatThrownBy(
                        () ->
                                service.recordCompletedOnboarding(
                                        7L, 5L, 99L, "Recorded during admin intake"))
                .isInstanceOf(ConflictException.class);
        verify(onboardingPromotionService, never()).promoteAllSteps(any(), any(), any(), any());
    }

    @Test
    void recordCompletedOnboardingRejectsWhenNotMember() {
        OnboardingInvitation invitation = invitation(5L, 20L, OnboardingInvitationStatus.OPEN);
        when(onboardingInvitationRepository.findById(5L)).thenReturn(Optional.of(invitation));
        when(userOrganizationMembershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(20L), eq(10L), any(Instant.class)))
                .thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.recordCompletedOnboarding(
                                        7L, 5L, 99L, "Recorded during admin intake"))
                .isInstanceOf(ConflictException.class);
        verify(onboardingPromotionService, never()).promoteAllSteps(any(), any(), any(), any());
    }

    private static OnboardingInvitation invitation(
            Long id, Long userId, OnboardingInvitationStatus status) {
        return OnboardingInvitation.builder()
                .id(id)
                .tenantId(7L)
                .userId(userId)
                .targetOrganizationId(10L)
                .status(status)
                .build();
    }

    private static User user(
            Long id, String firstName, String lastName, Long personalNumber, String email) {
        return User.builder()
                .id(id)
                .tenantId(7L)
                .firstName(firstName)
                .lastName(lastName)
                .personalNumber(personalNumber)
                .contactInformation(
                        UserContactInformation.builder()
                                .privateEmail(email)
                                .street("Old St")
                                .city("Old City")
                                .postalCode("12345")
                                .country("DE")
                                .build())
                .build();
    }
}
