/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.schedule;

import com.emberone.certification.service.CertificationReminderService;
import com.emberone.platform.tenant.TenantBatchRunner;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily sweep that raises certification reminder to-dos for missing, expiring, and expired
 * requirements, once per tenant.
 */
@Component
@RequiredArgsConstructor
public class CertificationReminderScheduler {

    private final TenantBatchRunner tenantBatchRunner;
    private final CertificationReminderService certificationReminderService;

    @Scheduled(cron = "${emberone.certifications.reminder-cron}", zone = "UTC")
    @SchedulerLock(name = "certificationReminder")
    public void raiseCertificationReminders() {
        tenantBatchRunner.forEachTenant(certificationReminderService::createRemindersForTenant);
    }
}
