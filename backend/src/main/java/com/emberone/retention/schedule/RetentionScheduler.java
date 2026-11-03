/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
