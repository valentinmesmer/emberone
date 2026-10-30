/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.schedule;

import com.emberone.courseoffering.service.CourseReminderService;
import com.emberone.platform.tenant.TenantBatchRunner;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily sweep that reminds participants and waitlisted users before a course starts, per tenant.
 */
@Component
@RequiredArgsConstructor
public class CourseOfferingReminderScheduler {

    private final TenantBatchRunner tenantBatchRunner;
    private final CourseReminderService reminderService;

    @Scheduled(cron = "${emberone.course-offerings.reminder-cron}", zone = "UTC")
    @SchedulerLock(name = "courseOfferingReminder")
    public void sendCourseOfferingReminders() {
        tenantBatchRunner.forEachTenant(reminderService::sendRemindersForTenant);
    }
}
