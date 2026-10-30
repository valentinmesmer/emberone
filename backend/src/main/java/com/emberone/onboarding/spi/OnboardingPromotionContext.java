/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.spi;

import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboardingdocument.domain.EntryDocumentFulfillmentChannel;

/** Context for promoting onboarding draft data into live user tables. */
public record OnboardingPromotionContext(
        OnboardingInvitation invitation,
        long tenantId,
        long userId,
        long organizationId,
        Long workflowReviewerUserId,
        EntryDocumentFulfillmentChannel entryDocumentChannel,
        String adminRecordReason) {

    public OnboardingPromotionContext(
            OnboardingInvitation invitation, long tenantId, long userId, long organizationId) {
        this(
                invitation,
                tenantId,
                userId,
                organizationId,
                null,
                EntryDocumentFulfillmentChannel.SELF,
                null);
    }

    public OnboardingPromotionContext(
            OnboardingInvitation invitation,
            long tenantId,
            long userId,
            long organizationId,
            Long workflowReviewerUserId) {
        this(
                invitation,
                tenantId,
                userId,
                organizationId,
                workflowReviewerUserId,
                EntryDocumentFulfillmentChannel.SELF,
                null);
    }

    public long invitationId() {
        return invitation.getId();
    }
}
