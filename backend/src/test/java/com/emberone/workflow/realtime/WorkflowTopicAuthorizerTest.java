/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.realtime;

import static com.emberone.platform.security.EmberOneAppRole.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.platform.realtime.auth.EmberOnePrincipal;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.platform.security.WorkflowInstanceAccess;
import com.emberone.testsupport.JwtTestSupport;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class WorkflowTopicAuthorizerTest {

    @Mock private WorkflowInstanceAccess workflowInstanceAccess;

    @InjectMocks private WorkflowTopicAuthorizer authorizer;

    private EmberOnePrincipal participantPrincipal;
    private EmberOnePrincipal foreignTenantPrincipal;

    @BeforeEach
    void setUp() {
        Jwt jwt = JwtTestSupport.buildJwt(USER, 5L, 10L);
        participantPrincipal = new EmberOnePrincipal(5L, 10L, jwt);
        foreignTenantPrincipal = new EmberOnePrincipal(99L, 10L, jwt);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        MDC.clear();
    }

    @Test
    void supportsWorkflowDomainOnly() {
        assertThat(authorizer.supports("workflow")).isTrue();
        assertThat(authorizer.supports("appointment")).isFalse();
    }

    @Test
    void authorizesParticipant() {
        when(workflowInstanceAccess.canView(123L, 10L)).thenReturn(true);

        assertThat(authorizer.authorize(participantPrincipal, "workflow", Optional.of(123L)))
                .isTrue();

        assertThat(TenantContextHolder.getTenantId()).isNull();
        verify(workflowInstanceAccess).canView(123L, 10L);
    }

    @Test
    void deniesNonParticipant() {
        when(workflowInstanceAccess.canView(123L, 10L)).thenReturn(false);

        assertThat(authorizer.authorize(participantPrincipal, "workflow", Optional.of(123L)))
                .isFalse();
    }

    @Test
    void deniesWhenEntityIdMissing() {
        assertThat(authorizer.authorize(participantPrincipal, "workflow", Optional.empty()))
                .isFalse();
    }

    @Test
    void setsTenantContextForAuthorization() {
        when(workflowInstanceAccess.canView(123L, 10L))
                .thenAnswer(
                        invocation -> {
                            assertThat(TenantContextHolder.getTenantId()).isEqualTo(5L);
                            assertThat(TenantContextHolder.getUserId()).isEqualTo(10L);
                            assertThat(MDC.get("tenantId")).isEqualTo("5");
                            assertThat(MDC.get("userId")).isEqualTo("10");
                            return true;
                        });

        assertThat(authorizer.authorize(participantPrincipal, "workflow", Optional.of(123L)))
                .isTrue();
    }

    @Test
    void foreignTenantHandledByTopicAuthorizationServiceNotAuthorizer() {
        when(workflowInstanceAccess.canView(123L, 10L)).thenReturn(false);

        assertThat(authorizer.authorize(foreignTenantPrincipal, "workflow", Optional.of(123L)))
                .isFalse();
    }
}
