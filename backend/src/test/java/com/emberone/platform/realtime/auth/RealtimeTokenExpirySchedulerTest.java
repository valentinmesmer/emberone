/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.auth;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

@ExtendWith(MockitoExtension.class)
class RealtimeTokenExpirySchedulerTest {

    @Mock private RealtimeSessionRegistry sessionRegistry;

    private RealtimeTokenExpiryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new RealtimeTokenExpiryScheduler(sessionRegistry);
    }

    @Test
    void closeExpiredSessionsDelegatesToRegistry() {
        scheduler.closeExpiredSessions();

        verify(sessionRegistry)
                .closeExpiredSessions(org.mockito.ArgumentMatchers.any(Instant.class));
    }

    @Test
    void registryClosesOpenWebSocketSessionWhenTokenExpired() throws IOException {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);

        registry.registerWebSocketSession("session-42", session);
        registry.register("session-42", Instant.now().minusSeconds(30));

        registry.closeExpiredSessions(Instant.now());

        verify(session)
                .close(
                        new CloseStatus(
                                RealtimeCloseCodes.TOKEN_EXPIRED,
                                RealtimeCloseCodes.TOKEN_EXPIRED_REASON));
    }
}
