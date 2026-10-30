/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.publish;

import com.emberone.platform.realtime.event.RealtimeEvent;

/** JSON payload for {@link RealtimeRedisFanOut#CHANNEL} pub/sub fan-out across instances. */
public record RealtimeRedisEnvelope(
        Kind kind,
        Long tenantId,
        Long userId,
        String userQueue,
        String topicDestination,
        RealtimeEvent event) {

    public enum Kind {
        USER,
        TOPIC
    }

    static RealtimeRedisEnvelope forUser(
            Long tenantId, Long userId, String userQueue, RealtimeEvent event) {
        return new RealtimeRedisEnvelope(Kind.USER, tenantId, userId, userQueue, null, event);
    }

    static RealtimeRedisEnvelope forTopic(String topicDestination, RealtimeEvent event) {
        return new RealtimeRedisEnvelope(Kind.TOPIC, null, null, null, topicDestination, event);
    }
}
