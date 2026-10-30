/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.topic;

import static com.emberone.platform.security.EmberOneAppRole.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.emberone.platform.realtime.auth.EmberOnePrincipal;
import com.emberone.testsupport.JwtTestSupport;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class TopicAuthorizationServiceTest {

    @Mock private TopicAuthorizer workflowAuthorizer;

    private TopicAuthorizationService service;
    private EmberOnePrincipal principal;

    @BeforeEach
    void setUp() {
        service = new TopicAuthorizationService(List.of(workflowAuthorizer));
        Jwt jwt = JwtTestSupport.buildJwt(USER, 5L, 10L);
        principal = new EmberOnePrincipal(5L, 10L, jwt);
    }

    @Test
    void allowsUserQueueSubscriptions() {
        assertThat(service.authorizeSubscribe(principal, "/user/queue/notifications")).isTrue();
        verifyNoInteractions(workflowAuthorizer);
    }

    @Test
    void allowsTenantTopicForMatchingTenant() {
        assertThat(service.authorizeSubscribe(principal, "/topic/tenant.5.appointments")).isTrue();
        verifyNoInteractions(workflowAuthorizer);
    }

    @Test
    void rejectsTenantTopicForForeignTenant() {
        assertThat(service.authorizeSubscribe(principal, "/topic/tenant.99.appointments"))
                .isFalse();
        verifyNoInteractions(workflowAuthorizer);
    }

    @Test
    void rejectsUnknownTopicPattern() {
        assertThat(service.authorizeSubscribe(principal, "/topic/workflows/1")).isFalse();
        verifyNoInteractions(workflowAuthorizer);
    }

    @Test
    void routesEntityTopicToMatchingAuthorizer() {
        when(workflowAuthorizer.supports("workflow")).thenReturn(true);
        when(workflowAuthorizer.authorize(principal, "workflow", Optional.of(123L)))
                .thenReturn(true);

        assertThat(service.authorizeSubscribe(principal, "/topic/tenant.5.workflow.123")).isTrue();

        verify(workflowAuthorizer).authorize(principal, "workflow", Optional.of(123L));
    }

    @Test
    void deniesEntityTopicWhenNoAuthorizerMatches() {
        when(workflowAuthorizer.supports("workflow")).thenReturn(false);

        assertThat(service.authorizeSubscribe(principal, "/topic/tenant.5.workflow.123")).isFalse();
    }

    @Test
    void deniesEntityTopicWhenAuthorizerRejects() {
        when(workflowAuthorizer.supports("workflow")).thenReturn(true);
        when(workflowAuthorizer.authorize(principal, "workflow", Optional.of(123L)))
                .thenReturn(false);

        assertThat(service.authorizeSubscribe(principal, "/topic/tenant.5.workflow.123")).isFalse();
    }
}
