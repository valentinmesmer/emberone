/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class AiRateLimiterTest {

    @Test
    void blocksAboveLimitPerUserAndRecoversAfterWindow() {
        AiProperties properties = new AiProperties();
        properties.setRateLimitPerMinute(2);
        AtomicLong now = new AtomicLong(1_000_000L);
        AiRateLimiter limiter = new AiRateLimiter(properties, now::get);

        limiter.acquire(1L);
        limiter.acquire(1L);
        assertThatThrownBy(() -> limiter.acquire(1L)).isInstanceOf(AiRateLimitException.class);
        // other user unaffected
        assertThatCode(() -> limiter.acquire(2L)).doesNotThrowAnyException();

        now.addAndGet(60_001L);
        assertThatCode(() -> limiter.acquire(1L)).doesNotThrowAnyException();
    }

    @Test
    void zeroLimitDisablesLimiting() {
        AiProperties properties = new AiProperties();
        properties.setRateLimitPerMinute(0);
        AiRateLimiter limiter = new AiRateLimiter(properties, () -> 0L);

        for (int i = 0; i < 100; i++) {
            limiter.acquire(1L);
        }
    }
}
