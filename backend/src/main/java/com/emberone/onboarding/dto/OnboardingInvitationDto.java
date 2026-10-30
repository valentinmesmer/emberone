/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.dto;

import com.emberone.onboarding.domain.OnboardingInvitationStatus;
import com.emberone.platform.time.OrganizationWallClock;
import com.emberone.user.domain.Salutation;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingInvitationDto {
    private Long id;
    private Long userId;
    private Long personalNumber;
    private Salutation salutation;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String privateEmail;
    private String street;
    private String city;
    private String postalCode;
    private String country;
    private OnboardingInvitationStatus status;
    @OrganizationWallClock private Instant invitedAt;
}
