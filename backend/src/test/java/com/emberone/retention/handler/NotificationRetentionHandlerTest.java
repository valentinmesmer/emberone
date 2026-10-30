/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.repository.NotificationRetentionRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationRetentionHandlerTest {

    @Mock NotificationRetentionRepository notificationRetentionRepository;

    @InjectMocks NotificationRetentionHandler handler;

    @Test
    void categoryIsNotification() {
        assertThat(handler.category()).isEqualTo(RetentionCategory.NOTIFICATION);
    }

    @Test
    void applyDelegatesToRepository() {
        Instant cutoff = Instant.parse("2020-01-01T00:00:00Z");
        when(notificationRetentionRepository.deleteNotificationsEligibleForRetention(cutoff))
                .thenReturn(4, 0);

        assertThat(handler.apply(cutoff)).isEqualTo(4);
        verify(notificationRetentionRepository, times(2))
                .deleteNotificationsEligibleForRetention(eq(cutoff));
    }
}
