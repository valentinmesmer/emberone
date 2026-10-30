/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.schedule;

import com.emberone.identity.service.IdentityDeprovisionService;
import com.emberone.platform.tenant.TenantBatchRunner;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdentityDeprovisionScheduler {

    private final TenantBatchRunner tenantBatchRunner;
    private final IdentityDeprovisionService identityDeprovisionService;

    @Scheduled(cron = "${emberone.identity.deprovision-cron}", zone = "UTC")
    @SchedulerLock(name = "identityDeprovision")
    public void processPendingDeprovisions() {
        tenantBatchRunner.forEachTenant(
                tenantId -> identityDeprovisionService.processPendingDeprovisions());
    }
}
