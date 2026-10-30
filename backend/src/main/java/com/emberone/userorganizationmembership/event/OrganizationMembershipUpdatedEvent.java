/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.event;

/** Published when an organization membership is updated (e.g. date range) without ending it. */
public record OrganizationMembershipUpdatedEvent(Long tenantId, Long userId, Long organizationId) {}
