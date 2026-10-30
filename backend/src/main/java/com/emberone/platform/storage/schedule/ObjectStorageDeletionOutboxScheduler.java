/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.storage.schedule;

import com.emberone.platform.storage.service.ObjectStorageDeletionOutboxService;
import com.emberone.platform.tenant.TenantBatchRunner;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ObjectStorageDeletionOutboxScheduler {

    private final TenantBatchRunner tenantBatchRunner;
    private final ObjectStorageDeletionOutboxService deletionOutboxService;

    @Scheduled(cron = "${emberone.object-storage.deletion-outbox-cron}", zone = "UTC")
    @SchedulerLock(name = "objectStorageDeletionOutbox")
    public void processDeletionOutbox() {
        tenantBatchRunner.forEachTenant(
                tenantId -> deletionOutboxService.processPendingDeletions());
        deletionOutboxService.processPendingDeletionsForDetachedTenants();
    }
}
