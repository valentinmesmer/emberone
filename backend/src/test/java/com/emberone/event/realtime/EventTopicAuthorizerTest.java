/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.realtime;

import static com.emberone.platform.security.EmberOneAppRole.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.event.domain.OperationEvent;
import com.emberone.event.domain.ServiceEvent;
import com.emberone.event.repository.OperationEventRepository;
import com.emberone.event.repository.ServiceEventRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.platform.realtime.auth.EmberOnePrincipal;
import com.emberone.platform.security.SecurityService;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.testsupport.JwtTestSupport;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class EventTopicAuthorizerTest {

    @Mock ServiceEventRepository serviceEventRepository;
    @Mock OperationEventRepository operationEventRepository;
    @Mock SecurityService securityService;

    @InjectMocks EventTopicAuthorizer authorizer;

    private EmberOnePrincipal principal;

    private static ServiceEvent serviceEvent(Long tenantId, Long id, Long organizationId) {
        Organization organization =
                Organization.builder().id(organizationId).tenantId(tenantId).build();
        return ServiceEvent.builder().id(id).tenantId(tenantId).organization(organization).build();
    }

    private static OperationEvent operationEvent(Long tenantId, Long id, Long organizationId) {
        Organization organization =
                Organization.builder().id(organizationId).tenantId(tenantId).build();
        return OperationEvent.builder()
                .id(id)
                .tenantId(tenantId)
                .organization(organization)
                .build();
    }

    @BeforeEach
    void setUp() {
        Jwt jwt = JwtTestSupport.buildJwt(USER, 5L, 10L);
        principal = new EmberOnePrincipal(5L, 10L, jwt);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void supportsServiceAndOperationDomains() {
        assertThat(authorizer.supports(EventTopicAuthorizer.SERVICE_DOMAIN)).isTrue();
        assertThat(authorizer.supports(EventTopicAuthorizer.OPERATION_DOMAIN)).isTrue();
        assertThat(authorizer.supports("workflow")).isFalse();
    }

    @Test
    void authorizesServiceEventReader() {
        ServiceEvent event = serviceEvent(5L, 7L, 20L);
        when(serviceEventRepository.findById(7L)).thenReturn(Optional.of(event));
        when(securityService.hasEventCategoryReadPermission(20L, "service")).thenReturn(true);

        assertThat(
                        authorizer.authorize(
                                principal, EventTopicAuthorizer.SERVICE_DOMAIN, Optional.of(7L)))
                .isTrue();
    }

    @Test
    void authorizesAttendanceReaderForOperationEvent() {
        OperationEvent event = operationEvent(5L, 9L, 30L);
        when(operationEventRepository.findById(9L)).thenReturn(Optional.of(event));
        when(securityService.hasEventCategoryReadPermission(30L, "operation")).thenReturn(false);
        when(securityService.hasEventAttendanceReadPermission(30L)).thenReturn(true);

        assertThat(
                        authorizer.authorize(
                                principal, EventTopicAuthorizer.OPERATION_DOMAIN, Optional.of(9L)))
                .isTrue();
    }
}
