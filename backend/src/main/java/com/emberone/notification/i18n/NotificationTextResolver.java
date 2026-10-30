/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.i18n;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.notification.domain.Notification;

/**
 * Resolves localized in-app notification text. Implemented by {@link NotificationMessageResolver}
 * and passed into {@link com.emberone.notification.channel.email.EmailRenderContext} so type
 * definitions can render e-mails without a circular dependency on the message resolver.
 */
public interface NotificationTextResolver {

    String resolveTitle(Notification notification, SupportedLocale locale);

    String resolveBody(Notification notification, SupportedLocale locale);
}
