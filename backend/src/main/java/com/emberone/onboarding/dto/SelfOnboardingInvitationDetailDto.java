/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.dto;

import com.emberone.onboarding.domain.OnboardingInvitationStatus;
import com.emberone.onboarding.domain.OnboardingStepType;
import com.emberone.platform.time.OrganizationWallClock;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SelfOnboardingInvitationDetailDto {
    Long id;

    /** Invite user - used to resolve promoted documents after approval. */
    Long subjectUserId;

    Long targetOrganizationId;
    String targetOrganizationName;
    OnboardingInvitationStatus status;
    @OrganizationWallClock Instant invitedAt;
    boolean editable;
    boolean allStepsFulfilled;
    boolean completionEnabled;

    /**
     * Configured steps already live-fulfilled without work in this invitation (omitted from {@code
     * steps}).
     */
    @Builder.Default List<OnboardingStepType> alreadyFulfilledStepTypes = List.of();

    String inputChangeMessage;
    List<OnboardingInvitationStepDto> steps;
    OnboardingInvitationCatalogDto catalog;
}
