/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.channel;

import com.emberone.notification.domain.Notification;
import com.emberone.notification.domain.NotificationChannel;
import com.emberone.notification.domain.NotificationDelivery;

public interface ChannelSender {

    boolean supports(NotificationChannel channel);

    void send(Notification notification, NotificationDelivery delivery) throws Exception;
}
