/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.schedule;

import com.emberone.notification.service.NotificationOutboxService;
import com.emberone.platform.tenant.TenantBatchRunner;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationOutboxScheduler {

    private final TenantBatchRunner tenantBatchRunner;
    private final NotificationOutboxService outboxService;

    @Scheduled(cron = "${emberone.notifications.outbox-cron}", zone = "UTC")
    @SchedulerLock(name = "notificationOutbox")
    public void processOutbox() {
        tenantBatchRunner.forEachTenant(tenantId -> outboxService.processPendingDeliveries());
    }
}
