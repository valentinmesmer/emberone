/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.userorganizationmembership.event;

import java.time.Instant;

/**
 * Published when an organization membership ends so dependent group memberships and other modules
 * can react without tight coupling.
 */
public record OrganizationMembershipEndedEvent(
        Long tenantId, Long userId, Long organizationId, Instant endDate) {}
