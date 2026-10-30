/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.type.workflow;

import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.i18n.NotificationDisplaySupport;
import com.emberone.platform.mail.i18n.MailMessageResolver;
import org.springframework.stereotype.Component;

@Component
public class WorkflowOpenedNotificationDefinition
        extends AbstractWorkflowNotificationTypeDefinition {

    public WorkflowOpenedNotificationDefinition(
            NotificationDisplaySupport displaySupport, MailMessageResolver mailMessageResolver) {
        super(NotificationType.WORKFLOW_OPENED, false, displaySupport, mailMessageResolver);
    }
}
