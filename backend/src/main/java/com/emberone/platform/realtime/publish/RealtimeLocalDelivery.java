/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.publish;

import com.emberone.platform.realtime.auth.EmberOnePrincipal;
import com.emberone.platform.realtime.event.RealtimeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/** Delivers realtime events to STOMP clients connected to this JVM. */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "emberone.realtime", name = "enabled", havingValue = "true")
public class RealtimeLocalDelivery {

    private final SimpMessagingTemplate messagingTemplate;

    public void deliverToUser(Long tenantId, Long userId, String userQueue, RealtimeEvent event) {
        messagingTemplate.convertAndSendToUser(
                EmberOnePrincipal.formatName(tenantId, userId), userQueue, event);
        log.debug(
                "Delivered realtime event type={} to user {} queue {}",
                event.type(),
                userId,
                userQueue);
    }

    public void deliverToTopic(String destination, RealtimeEvent event) {
        messagingTemplate.convertAndSend(destination, event);
        log.debug("Delivered realtime event type={} to topic {}", event.type(), destination);
    }
}
