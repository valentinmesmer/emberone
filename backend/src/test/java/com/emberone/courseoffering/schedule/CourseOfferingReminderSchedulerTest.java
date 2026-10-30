/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.schedule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.emberone.courseoffering.service.CourseReminderService;
import com.emberone.platform.tenant.TenantBatchRunner;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class CourseOfferingReminderSchedulerTest {

    @Test
    void scheduledInvocationRunsReminderSweepPerTenant() {
        CourseReminderService service = mock(CourseReminderService.class);
        TenantBatchRunner batchRunner = mock(TenantBatchRunner.class);
        CourseOfferingReminderScheduler scheduler =
                new CourseOfferingReminderScheduler(batchRunner, service);

        doAnswer(
                        invocation -> {
                            Consumer<Long> action = invocation.getArgument(0);
                            action.accept(7L);
                            action.accept(8L);
                            return null;
                        })
                .when(batchRunner)
                .forEachTenant(any());

        scheduler.sendCourseOfferingReminders();

        verify(service).sendRemindersForTenant(7L);
        verify(service).sendRemindersForTenant(8L);
    }
}
