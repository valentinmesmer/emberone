/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

class AiRateLimiterTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);

    private AiRateLimiter limiterWithLimit(int limit) {
        AiProperties properties = new AiProperties();
        properties.setRateLimitPerMinute(limit);
        return new AiRateLimiter(properties, redis);
    }

    @SuppressWarnings("unchecked")
    private void returnCount(long userId, long count) {
        when(redis.execute(
                        any(RedisScript.class),
                        eq(List.of(AiRateLimiter.KEY_PREFIX + userId)),
                        eq("60000")))
                .thenReturn(count);
    }

    @Test
    void allowsCallsUpToTheLimitAndBlocksAbove() {
        AiRateLimiter limiter = limiterWithLimit(2);
        returnCount(1L, 2L);
        assertThatCode(() -> limiter.acquire(1L)).doesNotThrowAnyException();

        returnCount(1L, 3L);
        assertThatThrownBy(() -> limiter.acquire(1L)).isInstanceOf(AiRateLimitException.class);
    }

    @Test
    void countsPerUser() {
        AiRateLimiter limiter = limiterWithLimit(2);
        returnCount(1L, 3L);
        returnCount(2L, 1L);

        assertThatThrownBy(() -> limiter.acquire(1L)).isInstanceOf(AiRateLimitException.class);
        assertThatCode(() -> limiter.acquire(2L)).doesNotThrowAnyException();
    }

    @Test
    @SuppressWarnings("unchecked")
    void failsOpenWhenRedisIsUnavailable() {
        AiRateLimiter limiter = limiterWithLimit(1);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new RedisConnectionFailureException("down"));

        assertThatCode(() -> limiter.acquire(1L)).doesNotThrowAnyException();
        verify(redis).execute(any(RedisScript.class), anyList(), any(Object[].class));
    }

    @Test
    void zeroLimitDisablesLimiting() {
        AiRateLimiter limiter = limiterWithLimit(0);

        limiter.acquire(1L);

        verifyNoInteractions(redis);
    }
}
