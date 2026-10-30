/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.notification;

import com.emberone.notification.domain.NotificationParamKeys;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.service.NotificationRequest;
import com.emberone.notification.service.NotificationService;
import com.emberone.platform.navigation.domain.AppLink;
import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.todo.domain.Todo;
import com.emberone.todo.realtime.TodoRecipientResolver;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@RequiredArgsConstructor
public class TodoNotificationNotifier {

    private final NotificationService notificationService;
    private final TodoRecipientResolver recipientResolver;

    private static Map<String, Object> buildParams(Todo todo) {
        Map<String, Object> params = new HashMap<>();
        if (todo.getParams() != null) {
            params.putAll(todo.getParams());
        }
        params.put(NotificationParamKeys.TODO_TYPE_KEY, todo.getType().name());
        return params;
    }

    private static AppLink resolveLink(Todo todo) {
        if (todo.getLinkType() == null || todo.getLinkId() == null) {
            return null;
        }
        if (todo.getLinkType() == AppLinkType.WORKFLOW) {
            return AppLink.workflow(todo.getLinkId());
        }
        return new AppLink(todo.getLinkType(), todo.getLinkId());
    }

    public void notifyCreated(Todo todo) {
        notifyCreated(todo, recipientResolver.resolveRecipientUserIds(todo));
    }

    /**
     * Sends {@code TODO_ASSIGNED} only to the given recipients (e.g. a newly joined group member).
     */
    public void notifyCreated(Todo todo, Set<Long> recipientUserIds) {
        if (CollectionUtils.isEmpty(recipientUserIds)) {
            return;
        }
        notificationService.notify(
                NotificationRequest.builder()
                        .tenantId(todo.getTenantId())
                        .recipientUserIds(recipientUserIds)
                        .type(NotificationType.TODO_ASSIGNED)
                        .params(buildParams(todo))
                        .link(resolveLink(todo))
                        .build());
    }
}
