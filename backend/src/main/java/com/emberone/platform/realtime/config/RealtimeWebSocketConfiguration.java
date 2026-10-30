/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.config;

import com.emberone.platform.config.EmberOneCorsConfiguration;
import com.emberone.platform.realtime.auth.RealtimeAuthChannelInterceptor;
import com.emberone.platform.realtime.auth.RealtimeSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

@Configuration
@EnableScheduling
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "emberone.realtime", name = "enabled", havingValue = "true")
public class RealtimeWebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    private final RealtimeAuthChannelInterceptor authChannelInterceptor;
    private final RealtimeSessionRegistry sessionRegistry;
    private final EmberOneCorsConfiguration.CorsProperties corsProperties;
    private final TaskScheduler realtimeHeartbeatTaskScheduler;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        corsProperties.getAllowedOriginPatterns().toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[] {10_000, 10_000})
                .setTaskScheduler(realtimeHeartbeatTaskScheduler);
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(128 * 1024);
        registration.setSendTimeLimit(20 * 1000);
        registration.setSendBufferSizeLimit(512 * 1024);
        registration.addDecoratorFactory(
                new WebSocketHandlerDecoratorFactory() {
                    @Override
                    public WebSocketHandler decorate(WebSocketHandler handler) {
                        return new WebSocketHandlerDecorator(handler) {
                            @Override
                            public void afterConnectionEstablished(WebSocketSession session)
                                    throws Exception {
                                sessionRegistry.registerWebSocketSession(session.getId(), session);
                                super.afterConnectionEstablished(session);
                            }

                            @Override
                            public void afterConnectionClosed(
                                    WebSocketSession session, CloseStatus closeStatus)
                                    throws Exception {
                                sessionRegistry.unregister(session.getId());
                                super.afterConnectionClosed(session, closeStatus);
                            }
                        };
                    }
                });
    }
}
