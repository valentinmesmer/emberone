/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.userorganizationmembership.event;

/** Published when an organization membership is updated (e.g. date range) without ending it. */
public record OrganizationMembershipUpdatedEvent(Long tenantId, Long userId, Long organizationId) {}
