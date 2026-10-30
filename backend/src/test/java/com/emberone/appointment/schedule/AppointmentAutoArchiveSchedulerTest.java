/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.schedule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.appointment.service.AppointmentAutoArchiveService;
import com.emberone.platform.tenant.TenantBatchRunner;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class AppointmentAutoArchiveSchedulerTest {

    @Test
    void scheduledInvocationRunsArchivePerTenant() {
        AppointmentAutoArchiveService service = mock(AppointmentAutoArchiveService.class);
        TenantBatchRunner batchRunner = mock(TenantBatchRunner.class);
        AppointmentAutoArchiveScheduler scheduler =
                new AppointmentAutoArchiveScheduler(batchRunner, service);

        doAnswer(
                        invocation -> {
                            Consumer<Long> action = invocation.getArgument(0);
                            action.accept(7L);
                            action.accept(8L);
                            return null;
                        })
                .when(batchRunner)
                .forEachTenant(any());

        scheduler.archiveEndedAppointments();

        verify(batchRunner).forEachTenant(any());
        verify(service, times(2)).archiveEndedAppointments();
    }
}
