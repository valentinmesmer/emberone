/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.realtime;

import com.emberone.event.event.EventAttendanceAddedEvent;
import com.emberone.event.event.EventAttendanceRemovedEvent;
import com.emberone.event.event.EventChangedEvent;
import com.emberone.event.event.EventDeletingEvent;
import com.emberone.platform.event.AfterCommitCallback;
import com.emberone.platform.event.AfterCommitEventListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventRealtimeListener {

    private final EventRealtimeNotifier notifier;
    private final EventOrganizationResolver organizationResolver;

    @AfterCommitCallback
    public void onEventChanged(EventChangedEvent event) {
        notifier.notifyUpdated(
                event.tenantId(), event.organizationId(), event.eventType(), event.eventId());
    }

    @AfterCommitEventListener
    public void onEventDeleting(EventDeletingEvent event) {
        organizationResolver
                .resolveOrganizationId(event.eventType(), event.eventId())
                .ifPresent(
                        organizationId ->
                                notifier.notifyDeleted(
                                        event.tenantId(),
                                        organizationId,
                                        event.eventType(),
                                        event.eventId()));
    }

    @AfterCommitEventListener
    public void onAttendanceAdded(EventAttendanceAddedEvent event) {
        organizationResolver
                .resolveOrganizationId(event.eventType(), event.eventId())
                .ifPresent(
                        organizationId ->
                                notifier.notifyAttendanceChanged(
                                        event.tenantId(),
                                        organizationId,
                                        event.eventType(),
                                        event.eventId()));
    }

    @AfterCommitEventListener
    public void onAttendanceRemoved(EventAttendanceRemovedEvent event) {
        organizationResolver
                .resolveOrganizationId(event.eventType(), event.eventId())
                .ifPresent(
                        organizationId ->
                                notifier.notifyAttendanceChanged(
                                        event.tenantId(),
                                        organizationId,
                                        event.eventType(),
                                        event.eventId()));
    }
}
