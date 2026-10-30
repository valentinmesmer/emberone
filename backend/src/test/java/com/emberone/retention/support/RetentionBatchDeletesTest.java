/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RetentionBatchDeletesTest {

    @Test
    void deleteUntilExhaustedSumsBatches() {
        AtomicInteger calls = new AtomicInteger();
        int total =
                RetentionBatchDeletes.deleteUntilExhausted(
                        () -> {
                            int n = calls.incrementAndGet();
                            return n < 3 ? 100 : 0;
                        });

        assertThat(total).isEqualTo(200);
        assertThat(calls.get()).isEqualTo(3);
    }
}
