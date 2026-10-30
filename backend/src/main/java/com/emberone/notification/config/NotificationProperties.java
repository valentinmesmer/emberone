/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "emberone.notifications")
public class NotificationProperties {

    private String outboxCron = "0 * * * * *";
    private int maxDeliveryAttempts = 5;
    private int outboxBatchSize = 100;
    private Duration processingStaleAfter = Duration.ofMinutes(15);
    private Duration deliveryRetryBackoff = Duration.ofMinutes(1);
}
