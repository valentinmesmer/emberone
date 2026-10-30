/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.auth;

import static com.emberone.platform.security.EmberOneAppRole.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.platform.logging.RequestIdSupport;
import com.emberone.platform.realtime.topic.TopicAuthorizationService;
import com.emberone.testsupport.JwtTestSupport;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class RealtimeAuthChannelInterceptorTest {

    @Mock private JwtDecoder jwtDecoder;
    @Mock private TopicAuthorizationService topicAuthorizationService;
    @Mock private RealtimeSessionRegistry sessionRegistry;

    private RealtimeAuthChannelInterceptor interceptor;

    private static Jwt validJwt(Instant expiresAt) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("app_role", USER.getRoleName())
                .subject("user")
                .claim("tenantId", "7")
                .claim("userId", "42")
                .expiresAt(expiresAt)
                .build();
    }

    private static Message<byte[]> connectMessage(Map<String, List<String>> nativeHeaders) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId("session-1");
        nativeHeaders.forEach(
                (name, values) -> values.forEach(value -> accessor.setNativeHeader(name, value)));
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static Message<byte[]> subscribeMessage(
            EmberOnePrincipal principal, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(principal);
        accessor.setDestination(destination);
        accessor.setSessionId("session-1");
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @BeforeEach
    void setUp() {
        interceptor =
                new RealtimeAuthChannelInterceptor(
                        jwtDecoder, topicAuthorizationService, sessionRegistry);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void connectWithValidTokenSetsPrincipalAndRegistersSession() {
        Jwt jwt = validJwt(Instant.now().plusSeconds(3600));
        when(jwtDecoder.decode("valid-token")).thenReturn(jwt);

        Message<byte[]> message =
                connectMessage(
                        Map.of(
                                RealtimeAuthChannelInterceptor.AUTHORIZATION_HEADER,
                                List.of("Bearer valid-token")));

        interceptor.preSend(message, null);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        assertThat(accessor.getUser()).isInstanceOf(EmberOnePrincipal.class);
        EmberOnePrincipal principal = (EmberOnePrincipal) accessor.getUser();
        assertThat(principal.getTenantId()).isEqualTo(7L);
        assertThat(principal.getUserId()).isEqualTo(42L);
        assertThat(principal.getName()).isEqualTo("7:42");
        verify(sessionRegistry).register(eq("session-1"), any(Instant.class));
    }

    @Test
    void connectRejectsMissingToken() {
        Message<byte[]> message = connectMessage(Map.of());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(RealtimeAuthenticationException.class)
                .hasMessageContaining("Missing access token");
    }

    @Test
    void connectRejectsUserTokenWithoutTenant() {
        Jwt jwt = JwtTestSupport.buildJwt(USER, null, 42L);
        when(jwtDecoder.decode("no-tenant")).thenReturn(jwt);

        Message<byte[]> message =
                connectMessage(
                        Map.of(
                                RealtimeAuthChannelInterceptor.AUTHORIZATION_HEADER,
                                List.of("Bearer no-tenant")));

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(RealtimeAuthenticationException.class)
                .hasMessageContaining("not valid for user-facing API");
    }

    @Test
    void connectRejectsInvalidToken() {
        when(jwtDecoder.decode("bad-token")).thenThrow(new RuntimeException("invalid"));

        Message<byte[]> message =
                connectMessage(
                        Map.of(
                                RealtimeAuthChannelInterceptor.AUTHORIZATION_HEADER,
                                List.of("Bearer bad-token")));

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(RealtimeAuthenticationException.class)
                .hasMessageContaining("Invalid access token");
    }

    @Test
    void subscribeRejectsWhenTopicAuthorizationFails() {
        EmberOnePrincipal principal =
                new EmberOnePrincipal(7L, 42L, validJwt(Instant.now().plusSeconds(3600)));
        when(topicAuthorizationService.authorizeSubscribe(principal, "/topic/tenant.99.workflow.1"))
                .thenReturn(false);

        Message<byte[]> message = subscribeMessage(principal, "/topic/tenant.99.workflow.1");

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(RealtimeAuthorizationException.class)
                .hasMessageContaining("Not authorized");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void subscribeAllowsAuthorizedDestination() {
        EmberOnePrincipal principal =
                new EmberOnePrincipal(7L, 42L, validJwt(Instant.now().plusSeconds(3600)));
        when(topicAuthorizationService.authorizeSubscribe(principal, "/user/queue/notifications"))
                .thenReturn(true);

        Message<byte[]> message = subscribeMessage(principal, "/user/queue/notifications");

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isSameAs(message);
        verify(topicAuthorizationService)
                .authorizeSubscribe(principal, "/user/queue/notifications");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void subscribeSetsRequestIdMdcFromSessionDuringAuthorization() {
        EmberOnePrincipal principal =
                new EmberOnePrincipal(7L, 42L, validJwt(Instant.now().plusSeconds(3600)));
        when(topicAuthorizationService.authorizeSubscribe(
                        principal, "/topic/tenant.7.appointment.1"))
                .thenAnswer(
                        invocation -> {
                            assertThat(MDC.get(RequestIdSupport.MDC_KEY)).isEqualTo("session-1");
                            return true;
                        });

        interceptor.preSend(subscribeMessage(principal, "/topic/tenant.7.appointment.1"), null);

        assertThat(MDC.get(RequestIdSupport.MDC_KEY)).isNull();
    }

    @Test
    void subscribeBindsJwtIntoSecurityContextDuringAuthorization() {
        Jwt jwt = validJwt(Instant.now().plusSeconds(3600));
        EmberOnePrincipal principal = new EmberOnePrincipal(7L, 42L, jwt);
        AtomicReference<Authentication> seen = new AtomicReference<>();
        when(topicAuthorizationService.authorizeSubscribe(
                        principal, "/topic/tenant.7.appointment.1"))
                .thenAnswer(
                        invocation -> {
                            seen.set(SecurityContextHolder.getContext().getAuthentication());
                            return true;
                        });

        interceptor.preSend(subscribeMessage(principal, "/topic/tenant.7.appointment.1"), null);

        assertThat(seen.get()).isInstanceOf(JwtAuthenticationToken.class);
        assertThat(((JwtAuthenticationToken) seen.get()).getToken()).isSameAs(jwt);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void sendRejectsClientOriginatedMessages() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/user/2:57/queue/notifications");
        accessor.setLeaveMutable(true);
        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(RealtimeAuthorizationException.class)
                .hasMessageContaining("SEND");
    }

    @Test
    void unsubscribeIsAllowedWithoutAuthorizationCheck() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setLeaveMutable(true);
        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isSameAs(message);
    }

    @Test
    void disconnectUnregistersSession() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setSessionId("session-1");
        accessor.setLeaveMutable(true);
        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.preSend(message, null);

        verify(sessionRegistry).unregister("session-1");
    }

    @Test
    void extractBearerTokenStripsPrefix() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader(
                RealtimeAuthChannelInterceptor.AUTHORIZATION_HEADER, "Bearer abc.def.ghi");

        assertThat(RealtimeAuthChannelInterceptor.extractBearerToken(accessor))
                .isEqualTo("abc.def.ghi");
    }
}
