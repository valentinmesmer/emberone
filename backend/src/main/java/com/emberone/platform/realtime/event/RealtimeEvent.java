/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RealtimeEvent(
        String type, Map<String, Long> entityIds, Instant occurredAt, Object payload) {

    public static RealtimeEvent thin(String type, Map<String, Long> entityIds) {
        return new RealtimeEvent(type, entityIds, Instant.now(), null);
    }

    public static RealtimeEvent withPayload(
            String type, Map<String, Long> entityIds, Object payload) {
        return new RealtimeEvent(type, entityIds, Instant.now(), payload);
    }

    public boolean isThin() {
        return payload == null;
    }
}
