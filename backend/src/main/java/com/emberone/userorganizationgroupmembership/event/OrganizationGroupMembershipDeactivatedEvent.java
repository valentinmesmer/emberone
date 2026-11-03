/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.userorganizationgroupmembership.event;

/**
 * Published when a user is no longer an active member of an organization group and workflow
 * approver claims for that group should be released.
 *
 * <p>Pair of {@link OrganizationGroupMembershipActivatedEvent} for join/leave symmetry.
 */
public record OrganizationGroupMembershipDeactivatedEvent(
        Long tenantId, Long userId, Long organizationGroupId) {}
