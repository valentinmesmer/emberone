/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.handler;

import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.repository.NotificationRetentionRepository;
import com.emberone.retention.spi.RetentionHandler;
import com.emberone.retention.support.RetentionBatchDeletes;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link RetentionCategory#NOTIFICATION}: unread in-app rows expire {@code created_at + profile
 * period}; any read row ({@code read_at IS NOT NULL}) is removed on the next run.
 */
@Component
@RequiredArgsConstructor
public class NotificationRetentionHandler implements RetentionHandler {

    private static final Set<String> TABLES = Set.of("ntf_notifications", "ntf_deliveries");

    private final NotificationRetentionRepository notificationRetentionRepository;

    @Override
    public RetentionCategory category() {
        return RetentionCategory.NOTIFICATION;
    }

    @Override
    public Set<String> tables() {
        return TABLES;
    }

    @Override
    public int apply(Instant cutoff) {
        return RetentionBatchDeletes.deleteUntilExhausted(
                () ->
                        notificationRetentionRepository.deleteNotificationsEligibleForRetention(
                                cutoff));
    }
}
