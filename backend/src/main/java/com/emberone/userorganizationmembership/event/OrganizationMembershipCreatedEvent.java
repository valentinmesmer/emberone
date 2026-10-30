/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.event;

/**
 * Published when a user joins an organization so dependent membership side-effects (e.g. AllMembers
 * group sync) can be handled independently.
 */
public record OrganizationMembershipCreatedEvent(Long tenantId, Long userId, Long organizationId) {}
