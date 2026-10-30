/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.schedule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.appointment.service.AppointmentSeriesExtensionService;
import com.emberone.platform.tenant.TenantBatchRunner;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class AppointmentSeriesExtensionSchedulerTest {

    @Test
    void scheduledInvocationRunsExtensionPerTenant() {
        AppointmentSeriesExtensionService service = mock(AppointmentSeriesExtensionService.class);
        TenantBatchRunner batchRunner = mock(TenantBatchRunner.class);
        AppointmentSeriesExtensionScheduler scheduler =
                new AppointmentSeriesExtensionScheduler(batchRunner, service);

        doAnswer(
                        invocation -> {
                            Consumer<Long> action = invocation.getArgument(0);
                            action.accept(7L);
                            action.accept(8L);
                            return null;
                        })
                .when(batchRunner)
                .forEachTenant(any());

        scheduler.extendActiveSeries();

        verify(batchRunner).forEachTenant(any());
        verify(service, times(2)).extendActiveSeries();
    }
}
