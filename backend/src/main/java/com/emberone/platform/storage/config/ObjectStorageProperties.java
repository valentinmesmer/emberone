/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.storage.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "emberone.object-storage")
public class ObjectStorageProperties {

    private String deletionOutboxCron = "0 * * * * *";
    private int deletionOutboxBatchSize = 100;
    private int maxDeletionAttempts = 5;
    private Duration processingStaleAfter = Duration.ofMinutes(15);
    private Duration deletionRetryBackoff = Duration.ofMinutes(1);
}
