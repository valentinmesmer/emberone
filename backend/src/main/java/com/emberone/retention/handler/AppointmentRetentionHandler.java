/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.handler;

import com.emberone.platform.security.TenantContextHolder;
import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.repository.AppointmentRetentionRepository;
import com.emberone.retention.service.RetentionObjectStorageSupport;
import com.emberone.retention.spi.RetentionHandler;
import com.emberone.retention.support.RetentionBatchDeletes;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentRetentionHandler implements RetentionHandler {

    private static final Set<String> TABLES =
            Set.of(
                    "appointment_series",
                    "appointments",
                    "appointment_audience_groups",
                    "appointment_responses",
                    "appointment_reads",
                    "appointment_series_audience_groups");

    private final AppointmentRetentionRepository appointmentRetentionRepository;
    private final RetentionObjectStorageSupport retentionObjectStorageSupport;

    @Override
    public RetentionCategory category() {
        return RetentionCategory.APPOINTMENT;
    }

    @Override
    public Set<String> tables() {
        return TABLES;
    }

    @Override
    public int apply(Instant cutoff) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is required for retention");
        }
        List<String> storageKeys =
                appointmentRetentionRepository.findAttachmentStorageKeysForAppointmentsEndingBefore(
                        cutoff);
        for (String storageKey : storageKeys) {
            retentionObjectStorageSupport.enqueueAttachmentObject(tenantId, storageKey);
        }
        int deleted = 0;
        deleted +=
                RetentionBatchDeletes.deleteUntilExhausted(
                        () ->
                                appointmentRetentionRepository
                                        .deleteAttachmentsForAppointmentsEndingBefore(cutoff));
        deleted +=
                RetentionBatchDeletes.deleteUntilExhausted(
                        () ->
                                appointmentRetentionRepository.deleteAppointmentsEndingBefore(
                                        cutoff));
        deleted +=
                RetentionBatchDeletes.deleteUntilExhausted(
                        () -> appointmentRetentionRepository.deleteEmptySeriesEndingBefore(cutoff));
        return deleted;
    }
}
