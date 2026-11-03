/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.user.event;

/**
 * Published when a user requests a notification-email change that must be verified at the pending
 * address. {@code rawToken} is the plaintext verification token (not stored).
 */
public record NotificationEmailChangeRequestedEvent(
        Long tenantId, Long userId, String firstName, String pendingEmail, String rawToken) {}
