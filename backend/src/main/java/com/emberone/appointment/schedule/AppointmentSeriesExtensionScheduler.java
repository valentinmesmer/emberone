/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.appointment.schedule;

import com.emberone.appointment.service.AppointmentSeriesExtensionService;
import com.emberone.platform.tenant.TenantBatchRunner;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentSeriesExtensionScheduler {

    private final TenantBatchRunner tenantBatchRunner;
    private final AppointmentSeriesExtensionService appointmentSeriesExtensionService;

    @Scheduled(cron = "${emberone.appointments.series-extension-cron}", zone = "UTC")
    @SchedulerLock(name = "appointmentSeriesExtension")
    public void extendActiveSeries() {
        tenantBatchRunner.forEachTenant(
                tenantId -> appointmentSeriesExtensionService.extendActiveSeries());
    }
}
