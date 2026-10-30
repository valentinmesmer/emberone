/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.realtime;

import com.emberone.event.domain.EventType;
import com.emberone.platform.realtime.event.RealtimeEvent;
import com.emberone.platform.realtime.publish.RealtimePublisher;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventRealtimeNotifier {

    public static final String EVENT_UPDATED = "event.updated";
    public static final String EVENT_ATTENDANCE_CHANGED = "event.attendance.changed";
    public static final String EVENT_DELETED = "event.deleted";
    static final String LIST_DOMAIN = "events";

    private final ObjectProvider<RealtimePublisher> realtimePublisherProvider;

    static String topicDomain(EventType eventType) {
        return eventType == EventType.SERVICE
                ? EventTopicAuthorizer.SERVICE_DOMAIN
                : EventTopicAuthorizer.OPERATION_DOMAIN;
    }

    private static Map<String, Long> buildEntityIds(
            Long organizationId, EventType eventType, Long eventId) {
        Map<String, Long> entityIds = new LinkedHashMap<>();
        entityIds.put("eventId", eventId);
        entityIds.put("organizationId", organizationId);
        entityIds.put("eventTypeOrdinal", (long) eventType.ordinal());
        return entityIds;
    }

    public void notifyUpdated(
            Long tenantId, Long organizationId, EventType eventType, Long eventId) {
        publish(tenantId, organizationId, eventType, eventId, EVENT_UPDATED);
    }

    public void notifyAttendanceChanged(
            Long tenantId, Long organizationId, EventType eventType, Long eventId) {
        publish(tenantId, organizationId, eventType, eventId, EVENT_ATTENDANCE_CHANGED);
    }

    public void notifyDeleted(
            Long tenantId, Long organizationId, EventType eventType, Long eventId) {
        publish(tenantId, organizationId, eventType, eventId, EVENT_DELETED);
    }

    private void publish(
            Long tenantId,
            Long organizationId,
            EventType eventType,
            Long eventId,
            String eventTypeName) {
        RealtimePublisher publisher = realtimePublisherProvider.getIfAvailable();
        if (publisher == null || tenantId == null || organizationId == null || eventId == null) {
            return;
        }
        RealtimeEvent event =
                RealtimeEvent.thin(
                        eventTypeName, buildEntityIds(organizationId, eventType, eventId));
        publisher.publishToTopic(tenantId, topicDomain(eventType) + "." + eventId, event);
        publisher.publishToTopic(tenantId, LIST_DOMAIN, event);
    }
}
