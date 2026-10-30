/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.auth;

import com.emberone.platform.logging.RequestIdSupport;
import com.emberone.platform.realtime.topic.TopicAuthorizationService;
import com.emberone.platform.security.EmberOneJwtClaims;
import com.emberone.platform.security.EmberOneUserTokenClaims;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeAuthChannelInterceptor implements ChannelInterceptor {

    static final String AUTHORIZATION_HEADER = "Authorization";
    static final String BEARER_PREFIX = "Bearer ";
    private static final Set<StompCommand> ALLOWED_CLIENT_COMMANDS =
            EnumSet.of(
                    StompCommand.CONNECT,
                    StompCommand.SUBSCRIBE,
                    StompCommand.UNSUBSCRIBE,
                    StompCommand.DISCONNECT,
                    StompCommand.ACK,
                    StompCommand.NACK);
    private final JwtDecoder jwtDecoder;
    private final TopicAuthorizationService topicAuthorizationService;
    private final RealtimeSessionRegistry sessionRegistry;

    @Nullable static String extractBearerToken(StompHeaderAccessor accessor) {
        String nativeAuthorization = firstNativeHeader(accessor, AUTHORIZATION_HEADER);
        if (StringUtils.hasText(nativeAuthorization)) {
            return stripBearerPrefix(nativeAuthorization);
        }
        String simpAuthorization = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(simpAuthorization)) {
            return stripBearerPrefix(simpAuthorization);
        }
        return null;
    }

    @Nullable private static String firstNativeHeader(StompHeaderAccessor accessor, String headerName) {
        Map<String, List<String>> nativeHeaders = accessor.toNativeHeaderMap();
        List<String> values = nativeHeaders.get(headerName);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.getFirst();
    }

    @Nullable private static String stripBearerPrefix(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return trimmed.substring(BEARER_PREFIX.length()).trim();
        }
        return trimmed;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            handleSubscribe(accessor);
            return message;
        }

        if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            unregisterSession(accessor);
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (command != null && !ALLOWED_CLIENT_COMMANDS.contains(command)) {
            throw new RealtimeAuthorizationException(
                    "STOMP command " + command + " is not permitted");
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String token = extractBearerToken(accessor);
        if (!StringUtils.hasText(token)) {
            throw new RealtimeAuthenticationException("Missing access token on STOMP CONNECT");
        }

        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(token);
        } catch (RuntimeException ex) {
            throw new RealtimeAuthenticationException("Invalid access token on STOMP CONNECT", ex);
        }

        if (!EmberOneUserTokenClaims.isValidForUserFacingApi(jwt)) {
            throw new RealtimeAuthenticationException(
                    "Access token is not valid for user-facing API");
        }

        Long tenantId = EmberOneJwtClaims.resolveTenantId(jwt).orElse(null);
        Long userId = EmberOneJwtClaims.resolveUserId(jwt).orElse(null);
        if (tenantId == null || userId == null) {
            throw new RealtimeAuthenticationException(
                    "Access token must contain tenantId and userId");
        }

        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null) {
            throw new RealtimeAuthenticationException("Access token must contain exp claim");
        }

        EmberOnePrincipal principal = new EmberOnePrincipal(tenantId, userId, jwt);
        accessor.setUser(principal);
        registerSession(accessor, expiresAt);
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof EmberOnePrincipal principal)) {
            throw new RealtimeAuthenticationException("STOMP session is not authenticated");
        }
        // Broker threads have no HTTP SecurityContext; bind the STOMP JWT so TokenService /
        // OrganizationAccessService resolve the same principal as REST requests.
        String requestId =
                RequestIdSupport.resolveCorrelationId(
                        firstNativeHeader(accessor, RequestIdSupport.HEADER_NAME),
                        accessor.getSessionId());
        RequestIdSupport.putMdc(requestId);
        SecurityContext previous = SecurityContextHolder.getContext();
        try {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new JwtAuthenticationToken(principal.getJwt()));
            SecurityContextHolder.setContext(context);

            String destination = accessor.getDestination();
            if (!topicAuthorizationService.authorizeSubscribe(principal, destination)) {
                log.warn(
                        "Rejected STOMP SUBSCRIBE to {} for principal {}",
                        destination,
                        principal.getName());
                throw new RealtimeAuthorizationException(
                        "Not authorized to subscribe to " + destination);
            }
        } finally {
            SecurityContextHolder.setContext(previous);
            RequestIdSupport.clearMdc();
        }
    }

    private void registerSession(StompHeaderAccessor accessor, Instant expiresAt) {
        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            sessionRegistry.register(sessionId, expiresAt);
        }
    }

    private void unregisterSession(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            sessionRegistry.unregister(sessionId);
        }
    }
}
