/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.drivinglicense.event;

/**
 * Published when a tenant driving-license definition is hard-deleted, so other modules (e.g.
 * qualification prerequisite links) can clean up without the catalog service depending on them.
 */
public record TenantDrivingLicenseDefinitionDeletedEvent(Long tenantId, String configKey) {}
