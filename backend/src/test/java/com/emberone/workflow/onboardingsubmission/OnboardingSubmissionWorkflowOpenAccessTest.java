/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.onboardingsubmission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboarding.domain.OnboardingInvitationStatus;
import com.emberone.onboarding.repository.OnboardingInvitationRepository;
import com.emberone.platform.security.TokenService;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.exception.WorkflowConfigurationException;
import com.emberone.workflow.service.WorkflowDefinitionService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OnboardingSubmissionWorkflowOpenAccessTest {

    @Mock TokenService tokenService;
    @Mock OnboardingInvitationRepository onboardingInvitationRepository;
    @Mock WorkflowDefinitionService workflowDefinitionService;

    @InjectMocks OnboardingSubmissionWorkflowOpenAccess access;

    @Test
    void canSubmitRequiresAuthenticatedTenantUser() {
        when(tokenService.getUserId()).thenReturn(null);
        when(tokenService.getTenantId()).thenReturn(1L);

        assertThat(access.canSubmit(5L)).isFalse();
    }

    @Test
    void canSubmitChecksTargetOrganizationDefinition() {
        OnboardingInvitation invitation =
                OnboardingInvitation.builder()
                        .id(5L)
                        .tenantId(1L)
                        .userId(2L)
                        .targetOrganizationId(20L)
                        .status(OnboardingInvitationStatus.OPEN)
                        .build();

        when(tokenService.getUserId()).thenReturn(2L);
        when(tokenService.getTenantId()).thenReturn(1L);
        when(onboardingInvitationRepository.findByIdAndUserId(5L, 2L))
                .thenReturn(Optional.of(invitation));
        when(workflowDefinitionService.resolveEffectiveDefinition(
                        WorkflowType.ONBOARDING_SUBMISSION.getId(), 20L))
                .thenReturn(WorkflowDefinition.builder().enabled(true).build());

        assertThat(access.canSubmit(5L)).isTrue();
    }

    @Test
    void canSubmitRejectsWhenInvitationNotOpen() {
        OnboardingInvitation invitation =
                OnboardingInvitation.builder()
                        .id(5L)
                        .tenantId(1L)
                        .userId(2L)
                        .targetOrganizationId(20L)
                        .status(OnboardingInvitationStatus.SUBMITTED)
                        .build();

        when(tokenService.getUserId()).thenReturn(2L);
        when(tokenService.getTenantId()).thenReturn(1L);
        when(onboardingInvitationRepository.findByIdAndUserId(5L, 2L))
                .thenReturn(Optional.of(invitation));

        assertThat(access.canSubmit(5L)).isFalse();
    }

    @Test
    void canSubmitRejectsWhenWorkflowDefinitionMissing() {
        OnboardingInvitation invitation =
                OnboardingInvitation.builder()
                        .id(5L)
                        .tenantId(1L)
                        .userId(2L)
                        .targetOrganizationId(20L)
                        .status(OnboardingInvitationStatus.OPEN)
                        .build();

        when(tokenService.getUserId()).thenReturn(2L);
        when(tokenService.getTenantId()).thenReturn(1L);
        when(onboardingInvitationRepository.findByIdAndUserId(5L, 2L))
                .thenReturn(Optional.of(invitation));
        when(workflowDefinitionService.resolveEffectiveDefinition(
                        WorkflowType.ONBOARDING_SUBMISSION.getId(), 20L))
                .thenThrow(new WorkflowConfigurationException("missing"));

        assertThat(access.canSubmit(5L)).isFalse();
    }
}
