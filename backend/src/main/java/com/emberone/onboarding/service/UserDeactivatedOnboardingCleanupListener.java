/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.service;

import com.emberone.user.event.UserDeactivatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/** Removes pending onboarding invitations and draft uploads when a user is deactivated. */
@Service
@Order(2)
@RequiredArgsConstructor
public class UserDeactivatedOnboardingCleanupListener {

    private final OnboardingInvitationService onboardingInvitationService;

    @EventListener
    public void onUserDeactivated(UserDeactivatedEvent event) {
        onboardingInvitationService.deletePendingForUser(event.userId());
    }
}
