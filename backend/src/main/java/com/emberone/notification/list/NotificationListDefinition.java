/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.list;

import com.emberone.notification.domain.Notification;
import com.emberone.platform.web.list.ListDefinition;

public final class NotificationListDefinition {

    public static final ListDefinition<Notification> INSTANCE =
            ListDefinition.<Notification>forEntity(Notification.class)
                    .sortKey("createdAt", "createdAt")
                    .defaultSort("createdAt,desc")
                    .build();

    private NotificationListDefinition() {}
}
