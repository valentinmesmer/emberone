/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.user.event;

/**
 * Published after a user has been created and persisted, so onboarding side-effects (group
 * membership, notifications, …) stay out of {@code UserService}.
 */
public record UserCreatedEvent(Long tenantId, Long userId, Long organizationId) {}
