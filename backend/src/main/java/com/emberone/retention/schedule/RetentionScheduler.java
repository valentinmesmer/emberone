/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.schedule;

import com.emberone.retention.service.RetentionExecutionService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RetentionScheduler {

    private final RetentionExecutionService retentionExecutionService;

    @Scheduled(cron = "${emberone.retention.cron}", zone = "UTC")
    @SchedulerLock(name = "retention")
    public void runRetention() {
        retentionExecutionService.runForAllTenants();
    }
}
