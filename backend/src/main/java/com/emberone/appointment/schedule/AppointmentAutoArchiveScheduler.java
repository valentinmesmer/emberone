/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.schedule;

import com.emberone.appointment.service.AppointmentAutoArchiveService;
import com.emberone.platform.tenant.TenantBatchRunner;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentAutoArchiveScheduler {

    private final TenantBatchRunner tenantBatchRunner;
    private final AppointmentAutoArchiveService appointmentAutoArchiveService;

    @Scheduled(cron = "${emberone.appointments.auto-archive-cron}", zone = "UTC")
    @SchedulerLock(name = "appointmentAutoArchive")
    public void archiveEndedAppointments() {
        tenantBatchRunner.forEachTenant(
                tenantId -> appointmentAutoArchiveService.archiveEndedAppointments());
    }
}
