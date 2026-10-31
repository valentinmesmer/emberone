/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.standard.event;

/**
 * Published when the standard assigned to an organization type changes. Organizations that inherit
 * the type default (no per-org override) may resolve a different effective standard afterward.
 */
public record OrganizationTypeStandardAssignedEvent(
        Long tenantId, Long organizationTypeId, Long previousStandardId, Long newStandardId) {}
