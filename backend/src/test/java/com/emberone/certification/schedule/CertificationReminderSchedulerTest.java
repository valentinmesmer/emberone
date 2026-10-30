/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.schedule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.certification.service.CertificationReminderService;
import com.emberone.platform.tenant.TenantBatchRunner;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class CertificationReminderSchedulerTest {

    @Test
    void scheduledInvocationRunsReminderSweepPerTenant() {
        CertificationReminderService service = mock(CertificationReminderService.class);
        TenantBatchRunner batchRunner = mock(TenantBatchRunner.class);
        CertificationReminderScheduler scheduler =
                new CertificationReminderScheduler(batchRunner, service);

        doAnswer(
                        invocation -> {
                            Consumer<Long> action = invocation.getArgument(0);
                            action.accept(7L);
                            action.accept(8L);
                            return null;
                        })
                .when(batchRunner)
                .forEachTenant(any());

        scheduler.raiseCertificationReminders();

        verify(batchRunner).forEachTenant(any());
        verify(service).createRemindersForTenant(7L);
        verify(service).createRemindersForTenant(8L);
    }
}
