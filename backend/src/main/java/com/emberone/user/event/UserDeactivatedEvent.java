/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.user.event;

/** Published after a user has been deactivated (memberships ended, IdP deprovision scheduled). */
public record UserDeactivatedEvent(Long tenantId, Long userId) {}
