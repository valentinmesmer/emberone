/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.onboardingsubmission;

import com.emberone.platform.security.TokenService;
import com.emberone.workflow.domain.WorkflowStatus;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.OnboardingSubmissionWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDto;
import com.emberone.workflow.repository.WorkflowRepository;
import com.emberone.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/workflows/onboarding-submission")
@RequiredArgsConstructor
public class OnboardingSubmissionWorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowRepository workflowRepository;
    private final TokenService tokenService;

    @PostMapping("/{invitationId}")
    @PreAuthorize("@securityService.canSubmitOnboardingInvitation(#invitationId)")
    public ResponseEntity<WorkflowDto> open(@PathVariable long invitationId) {
        Long openerId = tokenService.requireUserId();
        OnboardingSubmissionWorkflowPayload payload =
                OnboardingSubmissionWorkflowPayload.builder().invitationId(invitationId).build();
        WorkflowDto dto =
                workflowRepository
                        .findActiveBySubject(
                                openerId,
                                WorkflowType.ONBOARDING_SUBMISSION.getId(),
                                OnboardingSubmissionWorkflowHandler.SUBJECT_ENTITY_TYPE,
                                invitationId)
                        .filter(
                                workflow ->
                                        workflow.getStatus()
                                                == WorkflowStatus.AWAITING_INPUT_CHANGE)
                        .map(workflow -> workflowService.updatePayload(workflow.getId(), payload))
                        .orElseGet(
                                () ->
                                        workflowService.open(
                                                WorkflowType.ONBOARDING_SUBMISSION.getId(),
                                                payload,
                                                OnboardingSubmissionWorkflowHandler
                                                        .SUBJECT_ENTITY_TYPE,
                                                invitationId));
        return ResponseEntity.ok(dto);
    }
}
