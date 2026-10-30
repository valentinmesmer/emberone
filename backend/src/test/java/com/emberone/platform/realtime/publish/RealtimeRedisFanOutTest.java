/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.publish;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.emberone.platform.config.EmberOneJsonMapper;
import com.emberone.platform.realtime.event.RealtimeEvent;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class RealtimeRedisFanOutTest {

    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private RealtimeLocalDelivery localDelivery;

    private RealtimeRedisFanOut fanOut;

    @BeforeEach
    void setUp() {
        fanOut =
                new RealtimeRedisFanOut(
                        stringRedisTemplate, localDelivery, RealtimeRedisFanOut.CHANNEL);
    }

    @Test
    void onRedisMessageDeliversUserEnvelopeLocally() throws Exception {
        RealtimeEvent event = RealtimeEvent.thin("todo.created", Map.of("id", 1L));
        byte[] body =
                EmberOneJsonMapper.create()
                        .writeValueAsBytes(
                                RealtimeRedisEnvelope.forUser(
                                        3L, 9L, RealtimePublisher.TODOS_QUEUE, event));

        fanOut.onRedisMessage(body);

        verify(localDelivery).deliverToUser(3L, 9L, RealtimePublisher.TODOS_QUEUE, event);
    }

    @Test
    void onRedisMessageDeliversTopicEnvelopeLocally() throws Exception {
        RealtimeEvent event = RealtimeEvent.thin("appointment.updated", Map.of("id", 5L));
        byte[] body =
                EmberOneJsonMapper.create()
                        .writeValueAsBytes(
                                RealtimeRedisEnvelope.forTopic(
                                        "/topic/tenant.2.appointments", event));

        fanOut.onRedisMessage(body);

        verify(localDelivery).deliverToTopic("/topic/tenant.2.appointments", event);
    }

    @Test
    void onRedisMessageIgnoresMalformedPayload() {
        fanOut.onRedisMessage("{not-json".getBytes());

        verifyNoInteractions(localDelivery);
    }
}
