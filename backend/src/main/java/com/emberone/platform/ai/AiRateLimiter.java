/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Sliding-window limit of AI calls per user and minute.
 *
 * <p>PROTOTYPE: in-memory and per application instance, so with several instances the effective
 * limit is multiplied. Replace by a shared (Redis) limiter before production use with several
 * instances.
 */
@Component
public class AiRateLimiter {

    private static final long WINDOW_MILLIS = 60_000L;

    private final AiProperties properties;
    private final LongSupplier clockMillis;
    private final ConcurrentHashMap<Long, Deque<Long>> calls = new ConcurrentHashMap<>();

    @Autowired
    public AiRateLimiter(AiProperties properties) {
        this(properties, System::currentTimeMillis);
    }

    AiRateLimiter(AiProperties properties, LongSupplier clockMillis) {
        this.properties = properties;
        this.clockMillis = clockMillis;
    }

    /** Registers a call or throws {@link AiRateLimitException} when the user is over the limit. */
    public void acquire(long userId) {
        int limit = properties.getRateLimitPerMinute();
        if (limit <= 0) {
            return;
        }
        long now = clockMillis.getAsLong();
        boolean[] allowed = {true};
        calls.compute(
                userId,
                (id, deque) -> {
                    Deque<Long> window = deque == null ? new ArrayDeque<>() : deque;
                    while (!window.isEmpty() && now - window.peekFirst() >= WINDOW_MILLIS) {
                        window.pollFirst();
                    }
                    if (window.size() >= limit) {
                        allowed[0] = false;
                    } else {
                        window.addLast(now);
                    }
                    return window.isEmpty() ? null : window;
                });
        if (!allowed[0]) {
            throw new AiRateLimitException("Too many AI requests - please try again in a minute");
        }
    }
}
