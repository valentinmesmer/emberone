/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.type.identity;

import com.emberone.notification.channel.email.EmailRenderContext;
import com.emberone.notification.channel.email.EmailRenderResult;
import com.emberone.notification.domain.NotificationParamKeys;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.type.AbstractNotificationTypeDefinition;
import com.emberone.platform.mail.i18n.MailMessageResolver;
import org.springframework.stereotype.Component;

@Component
public class UsernameChangedNotificationDefinition extends AbstractNotificationTypeDefinition {

    private static final String TEMPLATE = "username-changed";

    public UsernameChangedNotificationDefinition(MailMessageResolver mailMessageResolver) {
        super(NotificationType.USERNAME_CHANGED, IN_APP_AND_EMAIL, mailMessageResolver);
    }

    @Override
    public EmailRenderResult renderEmail(EmailRenderContext context) {
        return new EmailRenderResult(
                context.textResolver().resolveTitle(context.notification(), context.locale()),
                TEMPLATE,
                mailMessageResolver.usernameChangedVariables(
                        context.locale(),
                        context.firstName(),
                        context.stringParam(NotificationParamKeys.PREVIOUS_USERNAME),
                        context.stringParam(NotificationParamKeys.NEW_USERNAME),
                        context.loginUrl()));
    }
}
