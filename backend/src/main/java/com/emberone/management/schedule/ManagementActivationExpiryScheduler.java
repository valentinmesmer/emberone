/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.schedule;

import com.emberone.management.service.ManagementActivationExpiryService;
import com.emberone.platform.tenant.TenantBatchRunner;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ManagementActivationExpiryScheduler {

    private final TenantBatchRunner tenantBatchRunner;
    private final ManagementActivationExpiryService expiryService;

    @Scheduled(fixedDelayString = "${emberone.management.expiry-check-interval-ms:15000}")
    @SchedulerLock(name = "managementActivationExpiry")
    public void notifyExpiredActivations() {
        tenantBatchRunner.forEachTenant(tenantId -> expiryService.notifyExpiredActivations());
    }
}
