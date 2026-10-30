/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.emberone.notification.domain.NotificationParamKeys;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.service.NotificationRequest;
import com.emberone.notification.service.NotificationService;
import com.emberone.platform.navigation.domain.AppLink;
import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.todo.domain.Todo;
import com.emberone.todo.domain.TodoParamKeys;
import com.emberone.todo.domain.TodoType;
import com.emberone.todo.realtime.TodoRecipientResolver;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TodoNotificationNotifierTest {

    private static final Long TENANT_ID = 1L;
    private static final Long WORKFLOW_ID = 42L;
    private static final Long ASSIGNEE_ID = 99L;

    @Mock NotificationService notificationService;
    @Mock TodoRecipientResolver recipientResolver;

    @InjectMocks TodoNotificationNotifier notifier;

    @Test
    void notifyCreatedSendsToResolvedRecipients() {
        Todo todo =
                Todo.builder()
                        .id(10L)
                        .tenantId(TENANT_ID)
                        .type(TodoType.WORKFLOW_START_REQUESTED)
                        .params(
                                Map.of(
                                        TodoParamKeys.PARTICIPANT_ID, 7L,
                                        TodoParamKeys.WORKFLOW_TYPE_KEY, "NAME_CHANGE",
                                        TodoParamKeys.OPENED_BY_DISPLAY_NAME, "Max Mustermann"))
                        .linkType(AppLinkType.WORKFLOW)
                        .linkId(WORKFLOW_ID)
                        .build();
        when(recipientResolver.resolveRecipientUserIds(todo)).thenReturn(Set.of(ASSIGNEE_ID, 50L));

        notifier.notifyCreated(todo);

        ArgumentCaptor<NotificationRequest> captor =
                ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).notify(captor.capture());
        NotificationRequest request = captor.getValue();
        assertThat(request.tenantId()).isEqualTo(TENANT_ID);
        assertThat(request.recipientUserIds()).containsExactlyInAnyOrder(ASSIGNEE_ID, 50L);
        assertThat(request.type()).isEqualTo(NotificationType.TODO_ASSIGNED);
        assertThat(request.link()).isEqualTo(AppLink.workflow(WORKFLOW_ID));
        assertThat(request.params())
                .containsEntry(NotificationParamKeys.TODO_TYPE_KEY, "WORKFLOW_START_REQUESTED");
        assertThat(request.params()).containsEntry(TodoParamKeys.WORKFLOW_TYPE_KEY, "NAME_CHANGE");
    }

    @Test
    void notifyCreatedWithExplicitRecipientsDoesNotResolve() {
        Todo todo =
                Todo.builder()
                        .id(10L)
                        .tenantId(TENANT_ID)
                        .type(TodoType.WORKFLOW_START_REQUESTED)
                        .params(Map.of(TodoParamKeys.WORKFLOW_TYPE_KEY, "NAME_CHANGE"))
                        .linkType(AppLinkType.WORKFLOW)
                        .linkId(WORKFLOW_ID)
                        .build();

        notifier.notifyCreated(todo, Set.of(ASSIGNEE_ID));

        ArgumentCaptor<NotificationRequest> captor =
                ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).notify(captor.capture());
        assertThat(captor.getValue().recipientUserIds()).containsExactly(ASSIGNEE_ID);
        verifyNoInteractions(recipientResolver);
    }
}
