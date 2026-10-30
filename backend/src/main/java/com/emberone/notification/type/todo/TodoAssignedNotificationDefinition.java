/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.type.todo;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.notification.domain.Notification;
import com.emberone.notification.domain.NotificationParamKeys;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.i18n.NotificationDisplaySupport;
import com.emberone.notification.type.AbstractNotificationTypeDefinition;
import com.emberone.platform.mail.i18n.MailMessageResolver;
import com.emberone.todo.domain.TodoParamKeys;
import com.emberone.todo.domain.TodoType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TodoAssignedNotificationDefinition extends AbstractNotificationTypeDefinition {

    private final NotificationDisplaySupport displaySupport;

    public TodoAssignedNotificationDefinition(
            NotificationDisplaySupport displaySupport, MailMessageResolver mailMessageResolver) {
        super(NotificationType.TODO_ASSIGNED, IN_APP_AND_EMAIL, mailMessageResolver);
        this.displaySupport = displaySupport;
    }

    @Override
    public Collection<String> derivedSourceKeys() {
        return List.of(
                NotificationParamKeys.TODO_TYPE_KEY,
                TodoParamKeys.WORKFLOW_TYPE_KEY,
                TodoParamKeys.STATUS);
    }

    @Override
    public String messageKeyPrefix(Notification notification) {
        Map<String, Object> params = notification.getParams();
        Object todoTypeKey =
                params != null ? params.get(NotificationParamKeys.TODO_TYPE_KEY) : null;
        String todoType =
                todoTypeKey instanceof String key
                        ? key
                        : TodoType.WORKFLOW_APPROVAL_REQUIRED.name();
        return "todo." + todoType;
    }

    @Override
    public void enrichDisplayParams(
            Map<String, Object> rawParams, Map<String, Object> display, SupportedLocale locale) {
        Object workflowTypeKey = rawParams.get(TodoParamKeys.WORKFLOW_TYPE_KEY);
        if (workflowTypeKey instanceof String key && StringUtils.hasText(key)) {
            display.put(
                    TodoParamKeys.WORKFLOW_TYPE_LABEL,
                    displaySupport.resolveWorkflowTypeLabel(key, locale));
        }
        Object certificationStatus = rawParams.get(TodoParamKeys.STATUS);
        if (certificationStatus instanceof String statusKey && StringUtils.hasText(statusKey)) {
            String label = displaySupport.resolveCertificationStatusLabel(statusKey, locale);
            if (StringUtils.hasText(label)) {
                display.put(TodoParamKeys.STATUS_LABEL, label);
            }
        }
    }
}
