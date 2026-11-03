/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.admin.tenant.event;

/** Published when tenant master data changes in ways that affect cached tenant-scoped lookups. */
public record TenantUpdatedEvent(Long tenantId) {}
