/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationgroupmembership.event;

/**
 * Published when a user becomes an active member of an organization group so open group-assigned
 * todos can notify and realtime-push that user.
 */
public record OrganizationGroupMembershipActivatedEvent(Long userId, Long organizationGroupId) {}
