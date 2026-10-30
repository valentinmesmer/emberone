/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.event;

/** Published after a per-organization standard override was set or cleared. */
public record OrganizationStandardOverrideChangedEvent(
        Long tenantId, Long organizationId, Long previousStandardId, Long newStandardId) {}
