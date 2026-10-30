/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.platform.security.TenantContextHolder;
import com.emberone.retention.repository.AppointmentRetentionRepository;
import com.emberone.retention.service.RetentionObjectStorageSupport;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentRetentionHandlerTest {

    @Mock AppointmentRetentionRepository appointmentRetentionRepository;
    @Mock RetentionObjectStorageSupport retentionObjectStorageSupport;

    @InjectMocks AppointmentRetentionHandler handler;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void enqueuesAttachmentDeletesBeforePurgingRows() {
        TenantContextHolder.setTenantId(7L);
        Instant cutoff = Instant.parse("2024-01-01T00:00:00Z");
        when(appointmentRetentionRepository.findAttachmentStorageKeysForAppointmentsEndingBefore(
                        cutoff))
                .thenReturn(List.of("tenants/7/appointments/1/a.pdf"));
        when(appointmentRetentionRepository.deleteAttachmentsForAppointmentsEndingBefore(cutoff))
                .thenReturn(1, 0);
        when(appointmentRetentionRepository.deleteAppointmentsEndingBefore(cutoff))
                .thenReturn(1, 0);
        when(appointmentRetentionRepository.deleteEmptySeriesEndingBefore(cutoff)).thenReturn(0);

        handler.apply(cutoff);

        verify(retentionObjectStorageSupport)
                .enqueueAttachmentObject(7L, "tenants/7/appointments/1/a.pdf");
    }
}
