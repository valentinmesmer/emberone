/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.domain;

/** Declaration order is the display order of the notification preferences. */
public enum NotificationType {
    TODO_ASSIGNED,
    WORKFLOW_APPROVED,
    WORKFLOW_REJECTED,
    WORKFLOW_OPENED,
    WORKFLOW_CANCELLED,
    WORKFLOW_INPUT_CHANGE_REQUESTED,
    USERNAME_CHANGED,
    COURSE_REGISTRATION_REMOVED,
    COURSE_OFFERING_CANCELLED,
    COURSE_OFFERING_SESSIONS_CHANGED,
    COURSE_OFFERING_START_REMINDER;
}
