/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.type.course;

import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.type.AbstractNotificationTypeDefinition;
import com.emberone.platform.mail.i18n.MailMessageResolver;
import org.springframework.stereotype.Component;

/** The sessions of an offering the user participates in or waits for changed. */
@Component
public class CourseOfferingSessionsChangedNotificationDefinition
        extends AbstractNotificationTypeDefinition {

    public CourseOfferingSessionsChangedNotificationDefinition(
            MailMessageResolver mailMessageResolver) {
        super(
                NotificationType.COURSE_OFFERING_SESSIONS_CHANGED,
                IN_APP_AND_EMAIL,
                mailMessageResolver);
    }
}
