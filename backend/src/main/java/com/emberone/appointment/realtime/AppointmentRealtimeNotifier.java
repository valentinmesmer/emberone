/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.realtime;

import com.emberone.appointment.domain.Appointment;
import com.emberone.platform.realtime.event.RealtimeEvent;
import com.emberone.platform.realtime.publish.RealtimePublisher;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentRealtimeNotifier {

    public static final String EVENT_UPDATED = "appointment.updated";
    static final String LIST_DOMAIN = "appointments";

    private final ObjectProvider<RealtimePublisher> realtimePublisherProvider;

    public void notifyUpdated(Appointment appointment) {
        if (appointment == null || appointment.getId() == null) {
            return;
        }
        Long organizationId =
                appointment.getOrganization() != null
                        ? appointment.getOrganization().getId()
                        : null;
        publish(appointment.getTenantId(), appointment.getId(), organizationId);
    }

    public void notifyUpdated(Long tenantId, Long organizationId, Long appointmentId) {
        if (tenantId == null || appointmentId == null) {
            return;
        }
        publish(tenantId, appointmentId, organizationId);
    }

    private void publish(Long tenantId, Long appointmentId, Long organizationId) {
        RealtimePublisher publisher = realtimePublisherProvider.getIfAvailable();
        if (publisher == null) {
            return;
        }
        Map<String, Long> entityIds = new LinkedHashMap<>();
        entityIds.put("appointmentId", appointmentId);
        if (organizationId != null) {
            entityIds.put("organizationId", organizationId);
        }
        RealtimeEvent event = RealtimeEvent.thin(EVENT_UPDATED, entityIds);
        publisher.publishToTopic(
                tenantId, AppointmentTopicAuthorizer.DOMAIN + "." + appointmentId, event);
        publisher.publishToTopic(tenantId, LIST_DOMAIN, event);
    }
}
