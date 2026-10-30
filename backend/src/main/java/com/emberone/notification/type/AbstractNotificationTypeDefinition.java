/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.type;

import com.emberone.notification.channel.email.EmailRenderContext;
import com.emberone.notification.channel.email.EmailRenderResult;
import com.emberone.notification.domain.NotificationChannel;
import com.emberone.notification.domain.NotificationType;
import com.emberone.platform.mail.i18n.MailMessageResolver;
import java.util.Set;

/**
 * Default e-mail rendering via the generic {@value DefaultNotificationEmail#TEMPLATE_NAME}
 * template. Subclasses override {@link #enrichDisplayParams} and/or {@link #renderEmail} when
 * needed.
 */
public abstract class AbstractNotificationTypeDefinition implements NotificationTypeDefinition {

    protected static final Set<NotificationChannel> IN_APP_AND_EMAIL =
            Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL);
    protected final MailMessageResolver mailMessageResolver;
    private final NotificationType type;
    private final Set<NotificationChannel> defaultChannels;

    protected AbstractNotificationTypeDefinition(
            NotificationType type,
            Set<NotificationChannel> defaultChannels,
            MailMessageResolver mailMessageResolver) {
        this.type = type;
        this.defaultChannels = Set.copyOf(defaultChannels);
        this.mailMessageResolver = mailMessageResolver;
    }

    @Override
    public NotificationType type() {
        return type;
    }

    @Override
    public Set<NotificationChannel> defaultChannels() {
        return defaultChannels;
    }

    @Override
    public EmailRenderResult renderEmail(EmailRenderContext context) {
        String title =
                context.textResolver().resolveTitle(context.notification(), context.locale());
        String body = context.textResolver().resolveBody(context.notification(), context.locale());
        return new EmailRenderResult(
                title,
                DefaultNotificationEmail.TEMPLATE_NAME,
                mailMessageResolver.notificationVariables(
                        context.locale(), title, body, context.loginUrl()));
    }
}
