/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.realtime;

import com.emberone.appointment.repository.AppointmentRepository;
import com.emberone.appointment.service.AppointmentAudienceService;
import com.emberone.platform.realtime.auth.EmberOnePrincipal;
import com.emberone.platform.realtime.topic.AbstractTenantScopedTopicAuthorizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentTopicAuthorizer extends AbstractTenantScopedTopicAuthorizer {

    static final String DOMAIN = "appointment";

    private final AppointmentRepository appointmentRepository;
    private final AppointmentAudienceService audienceService;

    @Override
    public boolean supports(String domain) {
        return DOMAIN.equals(domain);
    }

    @Override
    protected boolean authorizeEntity(EmberOnePrincipal principal, Long entityId) {
        return appointmentRepository
                .findById(entityId)
                .filter(appointment -> principal.getTenantId().equals(appointment.getTenantId()))
                .map(
                        appointment ->
                                audienceService.canUserAccessAppointment(
                                        principal.getUserId(), appointment))
                .orElse(false);
    }
}
