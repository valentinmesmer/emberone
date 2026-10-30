/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.exceptionhandler;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class AccessDeniedLogSupportTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(AccessDeniedLogSupport.class);
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        logger.setLevel(Level.WARN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        logger.detachAppender(logAppender);
    }

    @Test
    void logsRequestPathAndAuthenticatedUser() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("DELETE", "/api/v1/organizations/5/groups/3");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Jwt jwt =
                new Jwt(
                        "token",
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        Map.of("alg", "none"),
                        Map.of(
                                "userId", "42",
                                "tenantId", "7",
                                "app_role", "ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        AccessDeniedLogSupport.logAccessDenied(logger, "Access Denied");

        assertThat(logAppender.list).hasSize(1);
        assertThat(logAppender.list.getFirst().getFormattedMessage())
                .isEqualTo(
                        "Access denied: Access Denied [DELETE /api/v1/organizations/5/groups/3] userId=42 tenantId=7 appRole=ADMIN");
    }

    @Test
    void logsUnknownContextWhenRequestAndAuthenticationAreMissing() {
        AccessDeniedLogSupport.logAccessDenied(logger, "system group locked");

        assertThat(logAppender.list).hasSize(1);
        assertThat(logAppender.list.getFirst().getFormattedMessage())
                .isEqualTo(
                        "Access denied: system group locked [unknown unknown] userId=unknown tenantId=unknown appRole=unknown");
    }

    @Test
    void includesQueryStringInRequestPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
        request.setQueryString("organizationId=9");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        AccessDeniedLogSupport.logAccessDenied(logger, "denied");

        assertThat(logAppender.list.getFirst().getFormattedMessage())
                .contains("[GET /api/v1/users?organizationId=9]");
    }
}
