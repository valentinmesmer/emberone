/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.domain;

import java.util.List;

public enum OnboardingInvitationStatus {
    OPEN,
    SUBMITTED,
    APPROVED;

    /** Invite is in flight (new invitee or existing member with requested onboarding). */
    public static List<OnboardingInvitationStatus> pending() {
        return List.of(OPEN, SUBMITTED);
    }
}
