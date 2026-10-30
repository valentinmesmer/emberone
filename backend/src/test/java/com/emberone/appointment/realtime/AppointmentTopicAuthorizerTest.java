/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.realtime;

import static com.emberone.platform.security.EmberOneAppRole.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.emberone.appointment.domain.Appointment;
import com.emberone.appointment.repository.AppointmentRepository;
import com.emberone.appointment.service.AppointmentAudienceService;
import com.emberone.organization.domain.Organization;
import com.emberone.platform.realtime.auth.EmberOnePrincipal;
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
class AppointmentTopicAuthorizerTest {

    @Mock private AppointmentRepository appointmentRepository;

    @Mock private AppointmentAudienceService audienceService;

    @InjectMocks private AppointmentTopicAuthorizer authorizer;

    private EmberOnePrincipal principal;

    private static Appointment appointment(Long tenantId, Long id) {
        Organization organization = Organization.builder().id(20L).tenantId(tenantId).build();
        return Appointment.builder().id(id).tenantId(tenantId).organization(organization).build();
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
    void supportsAppointmentDomainOnly() {
        assertThat(authorizer.supports("appointment")).isTrue();
        assertThat(authorizer.supports("workflow")).isFalse();
    }

    @Test
    void authorizesAudienceMember() {
        Appointment appointment = appointment(5L, 7L);
        when(appointmentRepository.findById(7L)).thenReturn(Optional.of(appointment));
        when(audienceService.canUserAccessAppointment(10L, appointment)).thenReturn(true);

        assertThat(authorizer.authorize(principal, "appointment", Optional.of(7L))).isTrue();

        assertThat(TenantContextHolder.getTenantId()).isNull();
        verify(audienceService).canUserAccessAppointment(10L, appointment);
    }

    @Test
    void deniesUserOutsideAudience() {
        Appointment appointment = appointment(5L, 7L);
        when(appointmentRepository.findById(7L)).thenReturn(Optional.of(appointment));
        when(audienceService.canUserAccessAppointment(10L, appointment)).thenReturn(false);

        assertThat(authorizer.authorize(principal, "appointment", Optional.of(7L))).isFalse();
    }

    @Test
    void deniesWhenAppointmentMissing() {
        when(appointmentRepository.findById(7L)).thenReturn(Optional.empty());

        assertThat(authorizer.authorize(principal, "appointment", Optional.of(7L))).isFalse();
        verifyNoInteractions(audienceService);
    }

    @Test
    void deniesWhenAppointmentBelongsToForeignTenant() {
        Appointment appointment = appointment(99L, 7L);
        when(appointmentRepository.findById(7L)).thenReturn(Optional.of(appointment));

        assertThat(authorizer.authorize(principal, "appointment", Optional.of(7L))).isFalse();
        verifyNoInteractions(audienceService);
    }
}
