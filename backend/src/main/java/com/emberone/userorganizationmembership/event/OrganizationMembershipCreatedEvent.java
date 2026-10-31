/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.userorganizationmembership.event;

/**
 * Published when a user joins an organization so dependent membership side-effects (e.g. AllMembers
 * group sync) can be handled independently.
 */
public record OrganizationMembershipCreatedEvent(Long tenantId, Long userId, Long organizationId) {}
