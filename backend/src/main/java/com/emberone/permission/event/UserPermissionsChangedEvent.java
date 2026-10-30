/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permission.event;

/**
 * Published when a single user's effective permissions may have changed and the realtime client
 * should be told to refetch (e.g. group/organization membership create, update, or end).
 */
public record UserPermissionsChangedEvent(Long tenantId, Long userId) {}
