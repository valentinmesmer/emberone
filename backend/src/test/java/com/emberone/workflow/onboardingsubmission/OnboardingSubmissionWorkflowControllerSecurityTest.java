/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.onboardingsubmission;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.SecurityService;
import com.emberone.platform.security.TokenService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowStatus;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.WorkflowDto;
import com.emberone.workflow.repository.WorkflowRepository;
import com.emberone.workflow.service.WorkflowService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OnboardingSubmissionWorkflowController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class OnboardingSubmissionWorkflowControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean WorkflowService workflowService;

    @MockitoBean WorkflowRepository workflowRepository;

    @MockitoBean(name = "securityService")
    SecurityService workflowSecurityService;

    @Test
    void openOnboardingSubmissionRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/workflows/onboarding-submission/5"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openOnboardingSubmissionForbiddenWhenNotAllowed() throws Exception {
        when(workflowSecurityService.canSubmitOnboardingInvitation(5L)).thenReturn(false);

        mockMvc.perform(
                        post("/api/v1/auth/workflows/onboarding-submission/5")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void openOnboardingSubmissionAllowedWhenPermitted() throws Exception {
        when(workflowSecurityService.canSubmitOnboardingInvitation(5L)).thenReturn(true);
        when(workflowRepository.findActiveBySubject(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(workflowService.open(
                        eq(WorkflowType.ONBOARDING_SUBMISSION.getId()),
                        any(),
                        eq("ONBOARDING_INVITATION"),
                        eq(5L)))
                .thenReturn(WorkflowDto.builder().id(1L).build());

        mockMvc.perform(
                        post("/api/v1/auth/workflows/onboarding-submission/5")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void openOnboardingSubmissionFulfillsAwaitingInputChange() throws Exception {
        when(workflowSecurityService.canSubmitOnboardingInvitation(5L)).thenReturn(true);
        when(workflowRepository.findActiveBySubject(
                        eq(SecurityTestFixtures.USER_ID),
                        eq(WorkflowType.ONBOARDING_SUBMISSION.getId()),
                        eq("ONBOARDING_INVITATION"),
                        eq(5L)))
                .thenReturn(
                        Optional.of(
                                Workflow.builder()
                                        .id(9L)
                                        .status(WorkflowStatus.AWAITING_INPUT_CHANGE)
                                        .build()));
        when(workflowService.updatePayload(eq(9L), any()))
                .thenReturn(WorkflowDto.builder().id(9L).build());

        mockMvc.perform(
                        post("/api/v1/auth/workflows/onboarding-submission/5")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());

        verify(workflowService).updatePayload(eq(9L), any());
        verify(workflowService, never()).open(any(), any(), any(), any());
    }
}
