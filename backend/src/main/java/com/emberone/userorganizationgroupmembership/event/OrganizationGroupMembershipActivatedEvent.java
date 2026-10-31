/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.userorganizationgroupmembership.event;

/**
 * Published when a user becomes an active member of an organization group so open group-assigned
 * todos can notify and realtime-push that user.
 */
public record OrganizationGroupMembershipActivatedEvent(Long userId, Long organizationGroupId) {}
