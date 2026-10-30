/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.auth;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Component
public class RealtimeSessionRegistry {

    private final Map<String, SessionEntry> sessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> webSocketSessions = new ConcurrentHashMap<>();

    public void register(String sessionId, Instant tokenExpiresAt) {
        sessions.put(sessionId, new SessionEntry(tokenExpiresAt));
    }

    public void unregister(String sessionId) {
        sessions.remove(sessionId);
        webSocketSessions.remove(sessionId);
    }

    public void registerWebSocketSession(String sessionId, WebSocketSession session) {
        webSocketSessions.put(sessionId, session);
    }

    public void closeExpiredSessions(Instant now) {
        sessions.forEach(
                (sessionId, entry) -> {
                    if (!entry.tokenExpiresAt().isAfter(now)) {
                        closeWebSocketSession(sessionId);
                        sessions.remove(sessionId);
                    }
                });
    }

    private void closeWebSocketSession(String sessionId) {
        WebSocketSession session = webSocketSessions.remove(sessionId);
        if (session == null) {
            return;
        }
        if (!session.isOpen()) {
            return;
        }
        try {
            log.debug("Closing realtime session {} due to expired token", sessionId);
            session.close(
                    new CloseStatus(
                            RealtimeCloseCodes.TOKEN_EXPIRED,
                            RealtimeCloseCodes.TOKEN_EXPIRED_REASON));
        } catch (IOException ex) {
            log.warn("Failed to close expired realtime session {}", sessionId, ex);
        }
    }

    public record SessionEntry(Instant tokenExpiresAt) {}
}
