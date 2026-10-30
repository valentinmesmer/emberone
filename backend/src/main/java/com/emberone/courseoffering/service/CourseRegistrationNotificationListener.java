/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import com.emberone.courseoffering.event.CourseOfferingCancelledEvent;
import com.emberone.courseoffering.event.CourseOfferingSessionsChangedEvent;
import com.emberone.courseoffering.event.CourseRegistrationRemovedEvent;
import com.emberone.notification.domain.NotificationParamKeys;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.service.NotificationRequest;
import com.emberone.notification.service.NotificationService;
import com.emberone.platform.event.AfterCommitEventListener;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Tells users about changes the offering organization makes: an ended registration, a cancelled
 * offering and changed sessions.
 */
@Service
@RequiredArgsConstructor
public class CourseRegistrationNotificationListener {

    private final NotificationService notificationService;

    @AfterCommitEventListener
    public void onRegistrationRemoved(CourseRegistrationRemovedEvent event) {
        notificationService.notify(
                NotificationRequest.builder()
                        .tenantId(event.tenantId())
                        .recipientUserIds(Set.of(event.userId()))
                        .type(NotificationType.COURSE_REGISTRATION_REMOVED)
                        .params(
                                Map.of(
                                        NotificationParamKeys.COURSE_OFFERING_TOPIC,
                                        event.trainingCourseTypeName(),
                                        NotificationParamKeys.COURSE_REGISTRATION_REASON,
                                        event.reason()))
                        .build());
    }

    @AfterCommitEventListener
    public void onOfferingCancelled(CourseOfferingCancelledEvent event) {
        notificationService.notify(
                NotificationRequest.builder()
                        .tenantId(event.tenantId())
                        .recipientUserIds(event.userIds())
                        .type(NotificationType.COURSE_OFFERING_CANCELLED)
                        .params(
                                Map.of(
                                        NotificationParamKeys.COURSE_OFFERING_TOPIC,
                                        event.trainingCourseTypeName(),
                                        NotificationParamKeys.COURSE_OFFERING_CANCEL_REASON,
                                        event.reason()))
                        .build());
    }

    @AfterCommitEventListener
    public void onSessionsChanged(CourseOfferingSessionsChangedEvent event) {
        notificationService.notify(
                NotificationRequest.builder()
                        .tenantId(event.tenantId())
                        .recipientUserIds(event.userIds())
                        .type(NotificationType.COURSE_OFFERING_SESSIONS_CHANGED)
                        .params(
                                Map.of(
                                        NotificationParamKeys.COURSE_OFFERING_TOPIC,
                                        event.trainingCourseTypeName()))
                        .build());
    }
}
