/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emberone.notification.domain.NotificationChannel;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.i18n.NotificationDisplaySupport;
import com.emberone.notification.type.course.CourseOfferingCancelledNotificationDefinition;
import com.emberone.notification.type.course.CourseOfferingSessionsChangedNotificationDefinition;
import com.emberone.notification.type.course.CourseOfferingStartReminderNotificationDefinition;
import com.emberone.notification.type.course.CourseRegistrationRemovedNotificationDefinition;
import com.emberone.notification.type.identity.UsernameChangedNotificationDefinition;
import com.emberone.notification.type.todo.TodoAssignedNotificationDefinition;
import com.emberone.notification.type.workflow.*;
import com.emberone.platform.i18n.LocalizationService;
import com.emberone.platform.mail.i18n.MailMessageResolver;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationTypeDefinitionRegistryTest {

    @Mock MailMessageResolver mailMessageResolver;
    @Mock LocalizationService localizationService;

    @Test
    void registersAllNotificationTypes() {
        NotificationDisplaySupport displaySupport =
                new NotificationDisplaySupport(localizationService);
        NotificationTypeDefinitionRegistry registry =
                new NotificationTypeDefinitionRegistry(
                        List.of(
                                new WorkflowOpenedNotificationDefinition(
                                        displaySupport, mailMessageResolver),
                                new WorkflowApprovedNotificationDefinition(
                                        displaySupport, mailMessageResolver),
                                new WorkflowRejectedNotificationDefinition(
                                        displaySupport, mailMessageResolver),
                                new WorkflowCancelledNotificationDefinition(
                                        displaySupport, mailMessageResolver),
                                new WorkflowInputChangeRequestedNotificationDefinition(
                                        displaySupport, mailMessageResolver),
                                new TodoAssignedNotificationDefinition(
                                        displaySupport, mailMessageResolver),
                                new UsernameChangedNotificationDefinition(mailMessageResolver),
                                new CourseRegistrationRemovedNotificationDefinition(
                                        mailMessageResolver),
                                new CourseOfferingCancelledNotificationDefinition(
                                        mailMessageResolver),
                                new CourseOfferingSessionsChangedNotificationDefinition(
                                        mailMessageResolver),
                                new CourseOfferingStartReminderNotificationDefinition(
                                        mailMessageResolver)));

        for (NotificationType type : NotificationType.values()) {
            assertThat(registry.require(type).type()).isEqualTo(type);
        }
    }

    @Test
    void exposesDefaultChannelsPerType() {
        NotificationDisplaySupport displaySupport =
                new NotificationDisplaySupport(localizationService);
        NotificationTypeDefinitionRegistry registry =
                new NotificationTypeDefinitionRegistry(
                        List.of(
                                new WorkflowOpenedNotificationDefinition(
                                        displaySupport, mailMessageResolver),
                                new WorkflowApprovedNotificationDefinition(
                                        displaySupport, mailMessageResolver),
                                new WorkflowRejectedNotificationDefinition(
                                        displaySupport, mailMessageResolver),
                                new WorkflowCancelledNotificationDefinition(
                                        displaySupport, mailMessageResolver),
                                new WorkflowInputChangeRequestedNotificationDefinition(
                                        displaySupport, mailMessageResolver),
                                new TodoAssignedNotificationDefinition(
                                        displaySupport, mailMessageResolver),
                                new UsernameChangedNotificationDefinition(mailMessageResolver),
                                new CourseRegistrationRemovedNotificationDefinition(
                                        mailMessageResolver),
                                new CourseOfferingCancelledNotificationDefinition(
                                        mailMessageResolver),
                                new CourseOfferingSessionsChangedNotificationDefinition(
                                        mailMessageResolver),
                                new CourseOfferingStartReminderNotificationDefinition(
                                        mailMessageResolver)));

        assertThat(registry.require(NotificationType.USERNAME_CHANGED).defaultChannels())
                .containsExactlyInAnyOrder(NotificationChannel.IN_APP, NotificationChannel.EMAIL);
        assertThat(registry.require(NotificationType.WORKFLOW_APPROVED).defaultChannels())
                .containsExactlyInAnyOrder(NotificationChannel.IN_APP, NotificationChannel.EMAIL);
    }

    @Test
    void failsWhenDefinitionIsMissing() {
        assertThatThrownBy(() -> new NotificationTypeDefinitionRegistry(List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing NotificationTypeDefinition");
    }
}
