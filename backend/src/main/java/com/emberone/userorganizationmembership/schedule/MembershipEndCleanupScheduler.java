/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.schedule;

import com.emberone.platform.tenant.TenantBatchRunner;
import com.emberone.userorganizationgroupmembership.service.UserOrganizationGroupMembershipService;
import com.emberone.userorganizationmembership.service.UserOrganizationMembershipService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Minutely sweep that applies pending end-of-membership side effects. Rows with {@code endDate} in
 * the past and {@code endProcessedAt} unset are processed regardless of downtime duration.
 */
@Component
@RequiredArgsConstructor
public class MembershipEndCleanupScheduler {

    private final TenantBatchRunner tenantBatchRunner;
    private final UserOrganizationMembershipService organizationMembershipService;
    private final UserOrganizationGroupMembershipService groupMembershipService;

    @Scheduled(cron = "${emberone.memberships.end-cleanup-cron}", zone = "UTC")
    @SchedulerLock(name = "membershipEndCleanup")
    public void cleanupEndedMemberships() {
        tenantBatchRunner.forEachTenant(
                tenantId -> {
                    organizationMembershipService.processDueEndedMemberships();
                    groupMembershipService.processDueEndedMemberships();
                });
    }
}
