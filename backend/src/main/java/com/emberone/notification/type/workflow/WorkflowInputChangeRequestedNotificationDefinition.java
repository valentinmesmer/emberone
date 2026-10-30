/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.type.workflow;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.notification.domain.NotificationParamKeys;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.i18n.NotificationDisplaySupport;
import com.emberone.notification.type.AbstractNotificationTypeDefinition;
import com.emberone.platform.mail.i18n.MailMessageResolver;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WorkflowInputChangeRequestedNotificationDefinition
        extends AbstractNotificationTypeDefinition {

    private final NotificationDisplaySupport displaySupport;

    public WorkflowInputChangeRequestedNotificationDefinition(
            NotificationDisplaySupport displaySupport, MailMessageResolver mailMessageResolver) {
        super(
                NotificationType.WORKFLOW_INPUT_CHANGE_REQUESTED,
                IN_APP_AND_EMAIL,
                mailMessageResolver);
        this.displaySupport = displaySupport;
    }

    @Override
    public Collection<String> derivedSourceKeys() {
        return List.of(
                NotificationParamKeys.WORKFLOW_TYPE_KEY,
                NotificationParamKeys.INPUT_CHANGE_MESSAGE);
    }

    @Override
    public void enrichDisplayParams(
            Map<String, Object> rawParams, Map<String, Object> display, SupportedLocale locale) {
        Object workflowTypeKey = rawParams.get(NotificationParamKeys.WORKFLOW_TYPE_KEY);
        if (workflowTypeKey instanceof String key && StringUtils.hasText(key)) {
            display.put(
                    NotificationParamKeys.WORKFLOW_TYPE_LABEL,
                    displaySupport.resolveWorkflowTypeLabel(key, locale));
        }
        Object message = rawParams.get(NotificationParamKeys.INPUT_CHANGE_MESSAGE);
        if (message instanceof String text && StringUtils.hasText(text)) {
            display.put(NotificationParamKeys.INPUT_CHANGE_MESSAGE, text.trim());
        }
    }
}
