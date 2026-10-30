/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.event;

/**
 * Published once an identity user has been provisioned with a temporary password, so the welcome
 * mail can be sent independently of identity provisioning.
 */
public record UserWelcomeNeededEvent(
        Long tenantId,
        Long userId,
        String privateEmail,
        String firstName,
        String username,
        String temporaryPassword) {}
