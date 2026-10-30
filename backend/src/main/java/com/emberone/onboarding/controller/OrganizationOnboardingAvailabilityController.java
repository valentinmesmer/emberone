/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.controller;

import com.emberone.onboarding.dto.OnboardingStepsAvailableDto;
import com.emberone.onboarding.service.OnboardingInvitationReadinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}")
@RequiredArgsConstructor
public class OrganizationOnboardingAvailabilityController {

    private final OnboardingInvitationReadinessService onboardingInvitationReadinessService;

    @GetMapping("/onboarding-available")
    @PreAuthorize("@securityService.hasUserReadPermission(#organizationId)")
    public ResponseEntity<OnboardingStepsAvailableDto> getOnboardingAvailable(
            @PathVariable Long organizationId) {
        return ResponseEntity.ok(
                OnboardingStepsAvailableDto.builder()
                        .available(
                                onboardingInvitationReadinessService.hasConfiguredOnboardingSteps(
                                        organizationId))
                        .build());
    }
}
