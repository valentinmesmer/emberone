/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.event.domain.EventType;
import com.emberone.platform.security.TokenService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CertificationEventGrantNavigationResolverTest {

    @Mock CertificationEventGrantRepository grantRepository;
    @Mock TokenService tokenService;
    @InjectMocks CertificationEventGrantNavigationResolver resolver;

    @Test
    void resolvesServiceEventPathFromGrant() {
        when(tokenService.getTenantId()).thenReturn(1L);
        when(grantRepository.findById(9L))
                .thenReturn(
                        Optional.of(
                                CertificationEventGrant.builder()
                                        .id(9L)
                                        .organizationId(10L)
                                        .eventType(EventType.SERVICE)
                                        .eventId(5L)
                                        .build()));

        assertThat(resolver.resolve(9L))
                .isEqualTo(
                        new CertificationEventGrantNavigationResolver.Resolved(
                                "/organizations/10/department/service/5", EventType.SERVICE, 10L));
    }

    @Test
    void resolvesOperationEventPathFromGrant() {
        when(tokenService.getTenantId()).thenReturn(1L);
        when(grantRepository.findById(9L))
                .thenReturn(
                        Optional.of(
                                CertificationEventGrant.builder()
                                        .id(9L)
                                        .organizationId(10L)
                                        .eventType(EventType.OPERATION)
                                        .eventId(7L)
                                        .build()));

        assertThat(resolver.resolve(9L))
                .isEqualTo(
                        new CertificationEventGrantNavigationResolver.Resolved(
                                "/organizations/10/department/operation/7",
                                EventType.OPERATION,
                                10L));
    }

    @Test
    void returnsNullWhenGrantMissing() {
        when(tokenService.getTenantId()).thenReturn(1L);
        when(grantRepository.findById(9L)).thenReturn(Optional.empty());

        assertThat(resolver.resolve(9L)).isNull();
    }
}
