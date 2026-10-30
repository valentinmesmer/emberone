/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.event;

/** Published when tenant master data changes in ways that affect cached tenant-scoped lookups. */
public record TenantUpdatedEvent(Long tenantId) {}
