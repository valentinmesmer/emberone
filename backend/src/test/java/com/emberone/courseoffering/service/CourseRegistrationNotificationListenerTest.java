/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.emberone.courseoffering.event.CourseOfferingCancelledEvent;
import com.emberone.courseoffering.event.CourseOfferingSessionsChangedEvent;
import com.emberone.courseoffering.event.CourseRegistrationRemovedEvent;
import com.emberone.notification.domain.NotificationParamKeys;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.service.NotificationRequest;
import com.emberone.notification.service.NotificationService;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationNotificationListenerTest {

    @Mock NotificationService notificationService;

    @InjectMocks CourseRegistrationNotificationListener listener;

    @Test
    void notifiesTheRemovedUserWithTopicAndReason() {
        listener.onRegistrationRemoved(
                new CourseRegistrationRemovedEvent(7L, 3L, 100L, "Atemschutz", "Kein Bedarf"));

        ArgumentCaptor<NotificationRequest> captor =
                ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).notify(captor.capture());
        NotificationRequest request = captor.getValue();
        assertThat(request.tenantId()).isEqualTo(7L);
        assertThat(request.recipientUserIds()).containsExactly(3L);
        assertThat(request.type()).isEqualTo(NotificationType.COURSE_REGISTRATION_REMOVED);
        assertThat(request.params())
                .containsEntry(NotificationParamKeys.COURSE_OFFERING_TOPIC, "Atemschutz")
                .containsEntry(NotificationParamKeys.COURSE_REGISTRATION_REASON, "Kein Bedarf");
    }

    @Test
    void notifiesEveryoneConcernedOfACancelledOfferingInOneRequest() {
        listener.onOfferingCancelled(
                new CourseOfferingCancelledEvent(
                        7L, 100L, "Atemschutz", "Zu wenige", Set.of(3L, 4L, 5L)));

        ArgumentCaptor<NotificationRequest> captor =
                ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).notify(captor.capture());
        NotificationRequest request = captor.getValue();
        assertThat(request.tenantId()).isEqualTo(7L);
        assertThat(request.recipientUserIds()).containsExactlyInAnyOrder(3L, 4L, 5L);
        assertThat(request.type()).isEqualTo(NotificationType.COURSE_OFFERING_CANCELLED);
        assertThat(request.params())
                .containsEntry(NotificationParamKeys.COURSE_OFFERING_TOPIC, "Atemschutz")
                .containsEntry(NotificationParamKeys.COURSE_OFFERING_CANCEL_REASON, "Zu wenige");
    }

    @Test
    void notifiesParticipantsAndWaitlistOfChangedSessionsInOneRequest() {
        listener.onSessionsChanged(
                new CourseOfferingSessionsChangedEvent(7L, 100L, "Atemschutz", Set.of(3L, 4L)));

        ArgumentCaptor<NotificationRequest> captor =
                ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).notify(captor.capture());
        NotificationRequest request = captor.getValue();
        assertThat(request.recipientUserIds()).containsExactlyInAnyOrder(3L, 4L);
        assertThat(request.type()).isEqualTo(NotificationType.COURSE_OFFERING_SESSIONS_CHANGED);
        assertThat(request.params())
                .containsEntry(NotificationParamKeys.COURSE_OFFERING_TOPIC, "Atemschutz");
    }
}
