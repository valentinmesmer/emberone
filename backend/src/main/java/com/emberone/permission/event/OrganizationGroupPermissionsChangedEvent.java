/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permission.event;

/**
 * Published when a permission-group mapping for an organization group changed, so every currently
 * active member of that group should be told to refetch their permissions.
 */
public record OrganizationGroupPermissionsChangedEvent(Long tenantId, Long organizationGroupId) {}
