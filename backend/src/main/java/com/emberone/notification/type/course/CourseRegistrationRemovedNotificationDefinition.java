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

/** The offering organization removed the user's participant or waitlist place. */
@Component
public class CourseRegistrationRemovedNotificationDefinition
        extends AbstractNotificationTypeDefinition {

    public CourseRegistrationRemovedNotificationDefinition(
            MailMessageResolver mailMessageResolver) {
        super(NotificationType.COURSE_REGISTRATION_REMOVED, IN_APP_AND_EMAIL, mailMessageResolver);
    }
}
