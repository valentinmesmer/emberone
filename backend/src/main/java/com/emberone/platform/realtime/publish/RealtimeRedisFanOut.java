/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.publish;

import com.emberone.platform.config.EmberOneJsonMapper;
import com.emberone.platform.realtime.event.RealtimeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Publishes realtime envelopes to Redis; each instance subscribes and forwards locally via {@link
 * RealtimeLocalDelivery} (no duplicate delivery on the publishing instance).
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "emberone.realtime", name = "enabled", havingValue = "true")
public class RealtimeRedisFanOut {

    public static final String CHANNEL = "emberone:realtime";

    private final StringRedisTemplate stringRedisTemplate;
    private final RealtimeLocalDelivery localDelivery;
    private final JsonMapper jsonMapper;
    private final String channel;

    public RealtimeRedisFanOut(
            StringRedisTemplate stringRedisTemplate,
            RealtimeLocalDelivery localDelivery,
            @Value("${emberone.realtime.redis-channel:" + CHANNEL + "}") String channel) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.localDelivery = localDelivery;
        this.jsonMapper = EmberOneJsonMapper.create();
        this.channel = channel;
    }

    public void publishToUser(Long tenantId, Long userId, String userQueue, RealtimeEvent event) {
        publish(RealtimeRedisEnvelope.forUser(tenantId, userId, userQueue, event));
    }

    public void publishToTopic(String topicDestination, RealtimeEvent event) {
        publish(RealtimeRedisEnvelope.forTopic(topicDestination, event));
    }

    public void onRedisMessage(byte[] body) {
        if (body == null || body.length == 0) {
            return;
        }
        try {
            RealtimeRedisEnvelope envelope =
                    jsonMapper.readValue(body, RealtimeRedisEnvelope.class);
            deliverLocally(envelope);
        } catch (JacksonException ex) {
            log.warn("Ignoring malformed realtime Redis message: {}", ex.getMessage());
        }
    }

    private void publish(RealtimeRedisEnvelope envelope) {
        try {
            String json = jsonMapper.writeValueAsString(envelope);
            stringRedisTemplate.convertAndSend(channel, json);
            log.debug(
                    "Published realtime event type={} to Redis channel {}",
                    envelope.event().type(),
                    channel);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Failed to serialize realtime envelope", ex);
        }
    }

    private void deliverLocally(RealtimeRedisEnvelope envelope) {
        RealtimeEvent event = envelope.event();
        if (event == null) {
            log.warn("Ignoring realtime Redis envelope without event");
            return;
        }
        if (envelope.kind() == RealtimeRedisEnvelope.Kind.USER) {
            if (envelope.tenantId() == null
                    || envelope.userId() == null
                    || envelope.userQueue() == null) {
                log.warn("Ignoring incomplete user realtime Redis envelope");
                return;
            }
            localDelivery.deliverToUser(
                    envelope.tenantId(), envelope.userId(), envelope.userQueue(), event);
            return;
        }
        if (envelope.kind() == RealtimeRedisEnvelope.Kind.TOPIC) {
            if (envelope.topicDestination() == null) {
                log.warn("Ignoring topic realtime Redis envelope without destination");
                return;
            }
            localDelivery.deliverToTopic(envelope.topicDestination(), event);
        }
    }
}
