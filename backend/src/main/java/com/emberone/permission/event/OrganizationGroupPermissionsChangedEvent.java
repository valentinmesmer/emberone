/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.permission.event;

/**
 * Published when a permission-group mapping for an organization group changed, so every currently
 * active member of that group should be told to refetch their permissions.
 */
public record OrganizationGroupPermissionsChangedEvent(Long tenantId, Long organizationGroupId) {}
