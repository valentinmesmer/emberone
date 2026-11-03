/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.permission.event;

/**
 * Published when a permission group's own permissions changed, so every member reachable through
 * any organization group mapped to it should be told to refetch their permissions.
 */
public record PermissionGroupPermissionsChangedEvent(Long tenantId, Long permissionGroupId) {}
