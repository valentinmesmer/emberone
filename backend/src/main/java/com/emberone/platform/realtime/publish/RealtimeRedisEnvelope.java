/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
