/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

public class NotificationNotFoundException extends ResourceNotFoundException {

    public NotificationNotFoundException(Long notificationId) {
        super("Notification not found: " + notificationId);
    }
}
