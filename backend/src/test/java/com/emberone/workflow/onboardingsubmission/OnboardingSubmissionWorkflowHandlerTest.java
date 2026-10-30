/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.onboardingsubmission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboarding.domain.OnboardingInvitationStatus;
import com.emberone.onboarding.repository.OnboardingInvitationRepository;
import com.emberone.onboarding.service.OnboardingInvitationReadinessService;
import com.emberone.onboarding.service.OnboardingInvitationService;
import com.emberone.onboarding.service.OnboardingPromotionService;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.service.UserService;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowMode;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.domain.WorkflowTypeMapping;
import com.emberone.workflow.dto.OnboardingSubmissionWorkflowPayload;
import com.emberone.workflow.exception.WorkflowValidationException;
import com.emberone.workflow.repository.WorkflowDefinitionRepository;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import com.emberone.workflow.service.WorkflowTypeMappingService;
import com.emberone.workflow.spi.WorkflowOpenConcurrencyKeys;
import com.emberone.workflow.spi.WorkflowOpenContext;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class OnboardingSubmissionWorkflowHandlerTest {

    @Mock UserRepository userRepository;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock EntityAttachmentRepository attachmentRepository;
    @Mock OnboardingInvitationRepository onboardingInvitationRepository;
    @Mock OnboardingInvitationService onboardingInvitationService;
    @Mock OnboardingInvitationReadinessService onboardingInvitationReadinessService;
    @Mock OnboardingPromotionService onboardingPromotionService;
    @Mock UserService userService;
    @Mock WorkflowTypeMappingService workflowTypeMappingService;
    @Mock WorkflowDefinitionRepository workflowDefinitionRepository;
    @Mock LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport;

    private OnboardingSubmissionWorkflowHandler handler;

    @BeforeEach
    void setUp() {
        handler =
                new OnboardingSubmissionWorkflowHandler(
                        userRepository,
                        membershipRepository,
                        attachmentRepository,
                        documentPromotionMetadataSupport,
                        onboardingInvitationRepository,
                        onboardingInvitationService,
                        onboardingInvitationReadinessService,
                        onboardingPromotionService,
                        userService,
                        workflowTypeMappingService,
                        workflowDefinitionRepository,
                        JsonMapper.builder().build());
    }

    @Test
    void exposesStaticOnboardingSubmissionMetadata() {
        assertThat(handler.workflowType()).isEqualTo(WorkflowType.ONBOARDING_SUBMISSION);
        assertThat(handler.mode()).isEqualTo(WorkflowMode.STATIC);
        assertThat(handler.payloadType()).isEqualTo(OnboardingSubmissionWorkflowPayload.class);
        assertThat(handler.allowsMultipleConcurrentOpen()).isTrue();
        assertThat(handler.requiresOpenerOrganizationMembership()).isFalse();
        assertThat(handler.mayOpenWithoutActiveOrganizationMembership()).isTrue();
        assertThat(handler.listedInBeginnableCatalog()).isFalse();
        assertThat(handler.allowsInputChangeRequests()).isFalse();
        assertThat(handler.mapsRejectToInputChange()).isTrue();
        assertThat(handler.allowsOpenerFreeInputEdit()).isFalse();
    }

    @Test
    void resolveSecurityContextOrganizationUsesOpenInvitationWhenNoMembership() {
        OnboardingInvitation invitation =
                OnboardingInvitation.builder()
                        .id(5L)
                        .tenantId(7L)
                        .userId(1L)
                        .targetOrganizationId(20L)
                        .status(OnboardingInvitationStatus.OPEN)
                        .build();
        when(onboardingInvitationRepository.findFirstByUserIdAndStatusOrderByIdAsc(
                        1L, OnboardingInvitationStatus.OPEN))
                .thenReturn(Optional.of(invitation));

        assertThat(handler.resolveSecurityContextOrganization(1L, 7L)).isEqualTo(20L);
    }

    @Test
    void resolveSecurityContextOrganizationPrefersOpenInvitationOverMembership() {
        OnboardingInvitation invitation =
                OnboardingInvitation.builder()
                        .id(5L)
                        .tenantId(7L)
                        .userId(1L)
                        .targetOrganizationId(20L)
                        .status(OnboardingInvitationStatus.OPEN)
                        .build();
        when(onboardingInvitationRepository.findFirstByUserIdAndStatusOrderByIdAsc(
                        1L, OnboardingInvitationStatus.OPEN))
                .thenReturn(Optional.of(invitation));

        assertThat(handler.resolveSecurityContextOrganization(1L, 7L)).isEqualTo(20L);
    }

    @Test
    void resolveSecurityContextOrganizationFallsBackToMembershipWithoutOpenInvitation() {
        when(onboardingInvitationRepository.findFirstByUserIdAndStatusOrderByIdAsc(
                        1L, OnboardingInvitationStatus.OPEN))
                .thenReturn(Optional.empty());
        when(membershipRepository.findActiveByUser_IdAndMainTrue(eq(1L), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                com.emberone.userorganizationmembership.domain
                                        .UserOrganizationMembership.builder()
                                        .organization(
                                                com.emberone.organization.domain.Organization
                                                        .builder()
                                                        .id(10L)
                                                        .build())
                                        .build()));

        assertThat(handler.resolveSecurityContextOrganization(1L, 7L)).isEqualTo(10L);
    }

    @Test
    void resolveContextOrganizationUsesPayloadInvitationTargetOrganization() {
        OnboardingSubmissionWorkflowPayload payload =
                OnboardingSubmissionWorkflowPayload.builder().invitationId(5L).build();
        OnboardingInvitation invitation =
                OnboardingInvitation.builder()
                        .id(5L)
                        .tenantId(7L)
                        .userId(1L)
                        .targetOrganizationId(20L)
                        .status(OnboardingInvitationStatus.OPEN)
                        .build();
        when(onboardingInvitationRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.of(invitation));

        assertThat(handler.resolveContextOrganization(new WorkflowOpenContext<>(1L, 7L, payload)))
                .isEqualTo(20L);
    }

    @Test
    void validateOpenNormalizesInvitationAndChecksReadiness() {
        OnboardingSubmissionWorkflowPayload payload =
                OnboardingSubmissionWorkflowPayload.builder().invitationId(5L).build();
        OnboardingInvitation invitation =
                OnboardingInvitation.builder()
                        .id(5L)
                        .tenantId(7L)
                        .userId(1L)
                        .targetOrganizationId(20L)
                        .status(OnboardingInvitationStatus.OPEN)
                        .build();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));
        when(onboardingInvitationRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.of(invitation));
        when(membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(20L), any(Instant.class)))
                .thenReturn(false);
        when(onboardingInvitationReadinessService.isReadyToSubmit(1L, 5L)).thenReturn(true);
        when(workflowTypeMappingService.requireMapping(
                        20L, WorkflowType.ONBOARDING_SUBMISSION.getId()))
                .thenReturn(WorkflowTypeMapping.builder().build());

        handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, payload));

        assertThat(payload.getInvitationId()).isEqualTo(5L);
    }

    @Test
    void validateOpenRejectsWhenInvitationNotOpen() {
        OnboardingSubmissionWorkflowPayload payload =
                OnboardingSubmissionWorkflowPayload.builder().invitationId(5L).build();
        OnboardingInvitation invitation =
                OnboardingInvitation.builder()
                        .id(5L)
                        .tenantId(7L)
                        .userId(1L)
                        .targetOrganizationId(20L)
                        .status(OnboardingInvitationStatus.SUBMITTED)
                        .build();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));
        when(onboardingInvitationRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> handler.validateOpen(new WorkflowOpenContext<>(1L, 7L, payload)))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("not open");
    }

    @Test
    void resolveOpenConcurrencyKeysReturnsTargetOrganizationKey() {
        OnboardingSubmissionWorkflowPayload payload =
                OnboardingSubmissionWorkflowPayload.builder().invitationId(5L).build();
        OnboardingInvitation invitation =
                OnboardingInvitation.builder()
                        .id(5L)
                        .tenantId(7L)
                        .targetOrganizationId(20L)
                        .build();
        when(onboardingInvitationRepository.findById(5L)).thenReturn(Optional.of(invitation));

        assertThat(
                        handler.resolveOpenConcurrencyKeys(
                                new WorkflowOpenContext<>(1L, 7L, payload), null))
                .containsExactly(WorkflowOpenConcurrencyKeys.organization(20L));
    }

    @Test
    void onOpenedMarksInvitationSubmitted() {
        Workflow workflow =
                Workflow.builder()
                        .id(99L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .payload("{\"invitationId\":5}")
                        .build();

        handler.onOpened(workflow);

        verify(onboardingInvitationService).markSubmitted(7L, 1L, 5L);
    }

    @Test
    void onInputChangeRequestedReopensInvitation() {
        Workflow workflow =
                Workflow.builder()
                        .id(99L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .payload("{\"invitationId\":5}")
                        .build();

        handler.onInputChangeRequested(workflow);

        verify(onboardingInvitationService).reopen(7L, 1L, 5L);
    }

    @Test
    void onInputChangeWithdrawnMarksInvitationSubmitted() {
        Workflow workflow =
                Workflow.builder()
                        .id(99L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .payload("{\"invitationId\":5}")
                        .build();

        handler.onInputChangeWithdrawn(workflow);

        verify(onboardingInvitationService).markSubmitted(7L, 1L, 5L);
    }

    @Test
    void onInputUpdatedMarksInvitationSubmitted() {
        Workflow workflow =
                Workflow.builder()
                        .id(99L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .payload("{\"invitationId\":5}")
                        .build();

        handler.onInputUpdated(workflow);

        verify(onboardingInvitationService).markSubmitted(7L, 1L, 5L);
    }

    @Test
    void onApprovedCreatesMembershipPromotesAndMarksApproved() {
        OnboardingSubmissionWorkflowPayload payload =
                OnboardingSubmissionWorkflowPayload.builder().invitationId(5L).build();
        OnboardingInvitation invitation =
                OnboardingInvitation.builder()
                        .id(5L)
                        .tenantId(7L)
                        .userId(1L)
                        .targetOrganizationId(20L)
                        .build();
        Workflow workflow =
                Workflow.builder()
                        .id(99L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .payload("{\"invitationId\":5}")
                        .build();

        when(onboardingInvitationRepository.findById(5L)).thenReturn(Optional.of(invitation));

        handler.onApproved(workflow);

        verify(userService).ensureOrganizationMembershipAndAllMembersGroup(7L, 1L, 20L);
        verify(onboardingPromotionService).promoteAllSteps(invitation, null);
        verify(onboardingInvitationService).markApproved(7L, 5L);
    }

    @Test
    void onCancelledReopensInvitation() {
        OnboardingSubmissionWorkflowPayload payload =
                OnboardingSubmissionWorkflowPayload.builder().invitationId(5L).build();
        Workflow workflow =
                Workflow.builder()
                        .id(99L)
                        .tenantId(7L)
                        .openedByUserId(1L)
                        .payload("{\"invitationId\":5}")
                        .build();

        handler.onCancelled(workflow);

        verify(onboardingInvitationService).reopen(7L, 1L, 5L);
    }
}
