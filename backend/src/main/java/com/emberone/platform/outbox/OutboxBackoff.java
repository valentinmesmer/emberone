/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.outbox;

import java.time.Duration;
import java.time.Instant;

public final class OutboxBackoff {

    private OutboxBackoff() {}

    public static Instant nextAttemptAfter(int attempts, Duration baseBackoff) {
        long cappedSeconds =
                Math.min(baseBackoff.getSeconds() * attempts, Duration.ofHours(1).getSeconds());
        return Instant.now().plusSeconds(Math.max(cappedSeconds, baseBackoff.getSeconds()));
    }
}
