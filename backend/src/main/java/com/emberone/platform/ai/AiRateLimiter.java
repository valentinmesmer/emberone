/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * Limit of AI calls per user and minute, shared by all application instances through Redis.
 *
 * <p>Tumbling window: the first call of a user starts a one-minute window, later calls count
 * against it until it expires. The counter and its expiry are set in one Lua script, so a crash
 * between the two steps cannot leave a counter without expiry. Calls rejected by the limit are
 * counted too but do not extend the window.
 *
 * <p>Fails open: if Redis is unreachable the call is allowed and a warning is logged. The limit
 * protects cost and abuse of an optional feature; it must not take that feature down with Redis.
 */
@Slf4j
@Component
public class AiRateLimiter {

    static final String KEY_PREFIX = "aiRateLimit:";
    private static final long WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();
    private static final DefaultRedisScript<Long> COUNT_CALL =
            new DefaultRedisScript<>(
                    """
                    local count = redis.call('INCR', KEYS[1])
                    if count == 1 then
                        redis.call('PEXPIRE', KEYS[1], ARGV[1])
                    end
                    return count
                    """,
                    Long.class);

    private final AiProperties properties;
    private final StringRedisTemplate redisTemplate;

    public AiRateLimiter(AiProperties properties, StringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    /** Registers a call or throws {@link AiRateLimitException} when the user is over the limit. */
    public void acquire(long userId) {
        int limit = properties.getRateLimitPerMinute();
        if (limit <= 0) {
            return;
        }
        Long count;
        try {
            count =
                    redisTemplate.execute(
                            COUNT_CALL,
                            List.of(KEY_PREFIX + userId),
                            String.valueOf(WINDOW_MILLIS));
        } catch (RuntimeException ex) {
            log.warn("AI rate limit unavailable, allowing call: {}", ex.toString());
            return;
        }
        if (count != null && count > limit) {
            throw new AiRateLimitException("Too many AI requests - please try again in a minute");
        }
    }
}
