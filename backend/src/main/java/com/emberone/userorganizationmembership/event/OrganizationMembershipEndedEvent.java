/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.event;

import java.time.Instant;

/**
 * Published when an organization membership ends so dependent group memberships and other modules
 * can react without tight coupling.
 */
public record OrganizationMembershipEndedEvent(
        Long tenantId, Long userId, Long organizationId, Instant endDate) {}
