/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.service;

import static org.mockito.Mockito.verify;

import com.emberone.user.event.UserDeactivatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDeactivatedOnboardingCleanupListenerTest {

    private static final Long USER_ID = 42L;
    private static final Long TENANT_ID = 7L;

    @Mock OnboardingInvitationService onboardingInvitationService;

    @InjectMocks UserDeactivatedOnboardingCleanupListener listener;

    @Test
    void onUserDeactivatedRemovesPendingInvitationsForUser() {
        listener.onUserDeactivated(new UserDeactivatedEvent(TENANT_ID, USER_ID));

        verify(onboardingInvitationService).deletePendingForUser(USER_ID);
    }
}
