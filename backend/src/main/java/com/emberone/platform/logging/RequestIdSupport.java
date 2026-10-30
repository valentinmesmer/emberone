/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.logging;

import java.util.UUID;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;

/**
 * HTTP {@link #HEADER_NAME}, MDC {@link #MDC_KEY}, and safe accept/generate rules for correlation.
 */
@UtilityClass
public class RequestIdSupport {

    public static final String HEADER_NAME = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    private static final int MAX_LENGTH = 128;
    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9_-]+$");

    /**
     * Uses a trimmed incoming value when it matches the safe charset and length limit; otherwise
     * generates a new UUID.
     */
    public static String resolve(@Nullable String incoming) {
        if (incoming == null || incoming.isBlank()) {
            return generate();
        }
        String trimmed = incoming.trim();
        if (trimmed.length() <= MAX_LENGTH && SAFE.matcher(trimmed).matches()) {
            return trimmed;
        }
        return generate();
    }

    public static String generate() {
        return UUID.randomUUID().toString();
    }

    /**
     * Prefer {@code preferred} (e.g. STOMP {@link #HEADER_NAME}) when present; otherwise use {@code
     * fallback} (e.g. WebSocket session id); otherwise generate.
     */
    public static String resolveCorrelationId(
            @Nullable String preferred, @Nullable String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return resolve(preferred);
        }
        if (fallback != null && !fallback.isBlank()) {
            return resolve(fallback);
        }
        return generate();
    }

    public static void putMdc(String requestId) {
        MDC.put(MDC_KEY, requestId);
    }

    public static void clearMdc() {
        MDC.remove(MDC_KEY);
    }
}
