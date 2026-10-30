/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.handler;

import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.repository.EventRetentionRepository;
import com.emberone.retention.spi.RetentionHandler;
import com.emberone.retention.support.RetentionBatchDeletes;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventRetentionHandler implements RetentionHandler {

    private static final Set<String> TABLES =
            Set.of(
                    "service_events",
                    "operation_events",
                    "event_attendances",
                    "cert_event_grants",
                    "cert_event_grant_recipients");

    private final EventRetentionRepository eventRetentionRepository;

    @Override
    public RetentionCategory category() {
        return RetentionCategory.EVENT;
    }

    @Override
    public Set<String> tables() {
        return TABLES;
    }

    @Override
    public int apply(Instant cutoff) {
        int deleted = 0;
        deleted +=
                RetentionBatchDeletes.deleteUntilExhausted(
                        () ->
                                eventRetentionRepository
                                        .deleteCertEventGrantRecipientsForEventsBefore(cutoff));
        deleted +=
                RetentionBatchDeletes.deleteUntilExhausted(
                        () ->
                                eventRetentionRepository.deleteCertEventGrantsForEventsBefore(
                                        cutoff));
        deleted +=
                RetentionBatchDeletes.deleteUntilExhausted(
                        () ->
                                eventRetentionRepository.deleteEventAttendancesForEventsBefore(
                                        cutoff));
        deleted +=
                RetentionBatchDeletes.deleteUntilExhausted(
                        () -> eventRetentionRepository.deleteServiceEventsBefore(cutoff));
        deleted +=
                RetentionBatchDeletes.deleteUntilExhausted(
                        () -> eventRetentionRepository.deleteOperationEventsBefore(cutoff));
        return deleted;
    }
}
