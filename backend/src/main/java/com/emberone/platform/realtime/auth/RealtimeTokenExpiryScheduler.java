/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.auth;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "emberone.realtime", name = "enabled", havingValue = "true")
public class RealtimeTokenExpiryScheduler {

    private final RealtimeSessionRegistry sessionRegistry;

    /**
     * Intentionally not cluster-locked: {@link RealtimeSessionRegistry} is JVM-local; every
     * instance must close its own WebSocket sessions.
     */
    @Scheduled(fixedDelayString = "${emberone.realtime.token-expiry-check-interval-ms:30000}")
    public void closeExpiredSessions() {
        sessionRegistry.closeExpiredSessions(Instant.now());
    }
}
