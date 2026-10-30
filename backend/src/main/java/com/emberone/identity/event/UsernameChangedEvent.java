/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.event;

/**
 * Published when a user's login username changed, so a notification can be sent independently of
 * identity provisioning. {@code previousUsername} is empty when there was no previous username to
 * report.
 */
public record UsernameChangedEvent(
        Long tenantId, Long userId, String previousUsername, String newUsername) {}
