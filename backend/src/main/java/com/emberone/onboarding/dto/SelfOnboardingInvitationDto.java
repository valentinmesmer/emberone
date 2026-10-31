/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.onboarding.dto;

import com.emberone.onboarding.domain.OnboardingInvitationStatus;
import com.emberone.platform.time.OrganizationWallClock;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelfOnboardingInvitationDto {
    private Long id;
    private Long targetOrganizationId;
    private String targetOrganizationName;
    private OnboardingInvitationStatus status;
    @OrganizationWallClock private Instant invitedAt;
}
