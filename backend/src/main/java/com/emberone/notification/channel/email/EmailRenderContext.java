/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.channel.email;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.notification.domain.Notification;
import com.emberone.notification.i18n.NotificationTextResolver;
import java.util.Map;

public record EmailRenderContext(
        Notification notification,
        SupportedLocale locale,
        String firstName,
        String loginUrl,
        NotificationTextResolver textResolver) {

    public String stringParam(String key) {
        Map<String, Object> params = notification.getParams();
        if (params == null || params.get(key) == null) {
            return "";
        }
        return String.valueOf(params.get(key));
    }
}
