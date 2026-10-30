/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class RequestIdSupportTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void resolveAcceptsSafeIncomingValue() {
        assertThat(RequestIdSupport.resolve("  corr-abc_1  ")).isEqualTo("corr-abc_1");
    }

    @Test
    void resolveGeneratesWhenMissing() {
        String id = RequestIdSupport.resolve(null);
        assertThat(id).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void resolveGeneratesWhenUnsafeOrTooLong() {
        String unsafe = RequestIdSupport.resolve("bad id");
        assertThat(unsafe).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

        String tooLong = "a".repeat(129);
        assertThat(RequestIdSupport.resolve(tooLong))
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void resolveCorrelationIdPrefersHeaderOverSession() {
        assertThat(RequestIdSupport.resolveCorrelationId("hdr-1", "session-1")).isEqualTo("hdr-1");
    }

    @Test
    void resolveCorrelationIdUsesSessionWhenHeaderMissing() {
        assertThat(RequestIdSupport.resolveCorrelationId(null, "session-abc"))
                .isEqualTo("session-abc");
    }

    @Test
    void resolveCorrelationIdGeneratesWhenBothMissing() {
        assertThat(RequestIdSupport.resolveCorrelationId(null, null))
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void putAndClearMdc() {
        RequestIdSupport.putMdc("req-1");
        assertThat(MDC.get(RequestIdSupport.MDC_KEY)).isEqualTo("req-1");

        RequestIdSupport.clearMdc();
        assertThat(MDC.get(RequestIdSupport.MDC_KEY)).isNull();
    }
}
