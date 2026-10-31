/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.standard.event;

/** Published after a per-organization standard override was set or cleared. */
public record OrganizationStandardOverrideChangedEvent(
        Long tenantId, Long organizationId, Long previousStandardId, Long newStandardId) {}
