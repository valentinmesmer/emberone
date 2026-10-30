/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.event;

/**
 * Published after a child or root organization has been created. Sync listeners materialize
 * standard templates in the same transaction; after-commit listeners handle cache eviction and
 * other post-provisioning hooks.
 */
public record OrganizationCreatedEvent(
        Long tenantId, Long organizationId, Long organizationTypeId) {}
