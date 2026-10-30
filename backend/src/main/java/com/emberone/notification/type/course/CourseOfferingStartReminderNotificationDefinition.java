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

/** An offering the user participates in or waits for starts soon. */
@Component
public class CourseOfferingStartReminderNotificationDefinition
        extends AbstractNotificationTypeDefinition {

    public CourseOfferingStartReminderNotificationDefinition(
            MailMessageResolver mailMessageResolver) {
        super(
                NotificationType.COURSE_OFFERING_START_REMINDER,
                IN_APP_AND_EMAIL,
                mailMessageResolver);
    }
}
