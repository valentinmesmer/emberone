/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.controller;

import com.emberone.onboarding.dto.SelfOnboardingInvitationDetailDto;
import com.emberone.onboarding.service.OnboardingInvitationProgressService;
import com.emberone.onboarding.service.OnboardingInvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/users/{userId}")
@RequiredArgsConstructor
public class OnboardingInvitationUserOrganizationController {

    private final OnboardingInvitationService onboardingInvitationService;
    private final OnboardingInvitationProgressService onboardingInvitationProgressService;

    @GetMapping("/onboarding-invitation")
    @PreAuthorize("@securityService.hasUserReadPermission(#organizationId)")
    public ResponseEntity<SelfOnboardingInvitationDetailDto> getUserOnboardingInvitation(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        return ResponseEntity.ok(
                onboardingInvitationProgressService.getDetailForOrganization(
                        organizationId,
                        onboardingInvitationService
                                .requireInvitationForOrganizationUser(organizationId, userId)
                                .getId()));
    }
}
