/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.onboardingsubmission;

import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboarding.domain.OnboardingInvitationStatus;
import com.emberone.onboarding.repository.OnboardingInvitationRepository;
import com.emberone.platform.security.TokenService;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.exception.WorkflowConfigurationException;
import com.emberone.workflow.service.WorkflowDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pre-open authorization for onboarding submission. Uses the concrete invitation id (path param)
 * because generic {@link com.emberone.platform.security.WorkflowOpenAccess} cannot read the request
 * body and may resolve the wrong organization context.
 */
@Service
@RequiredArgsConstructor
public class OnboardingSubmissionWorkflowOpenAccess {

    private final TokenService tokenService;
    private final OnboardingInvitationRepository onboardingInvitationRepository;
    private final WorkflowDefinitionService workflowDefinitionService;

    @Transactional(readOnly = true)
    public boolean canSubmit(long invitationId) {
        Long userId = tokenService.getUserId();
        Long tenantId = tokenService.getTenantId();
        if (userId == null || tenantId == null) {
            return false;
        }
        return onboardingInvitationRepository
                .findByIdAndUserId(invitationId, userId)
                .filter(invitation -> invitation.getTenantId().equals(tenantId))
                .filter(invitation -> invitation.getStatus() == OnboardingInvitationStatus.OPEN)
                .map(this::hasEnabledWorkflowDefinition)
                .orElse(false);
    }

    private boolean hasEnabledWorkflowDefinition(OnboardingInvitation invitation) {
        try {
            WorkflowDefinition definition =
                    workflowDefinitionService.resolveEffectiveDefinition(
                            WorkflowType.ONBOARDING_SUBMISSION.getId(),
                            invitation.getTargetOrganizationId());
            return definition.isEnabled();
        } catch (WorkflowConfigurationException ex) {
            return false;
        }
    }
}
