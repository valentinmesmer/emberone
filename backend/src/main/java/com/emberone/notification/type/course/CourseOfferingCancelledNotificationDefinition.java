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

/** The offering organization cancelled the offering the user was registered for. */
@Component
public class CourseOfferingCancelledNotificationDefinition
        extends AbstractNotificationTypeDefinition {

    public CourseOfferingCancelledNotificationDefinition(MailMessageResolver mailMessageResolver) {
        super(NotificationType.COURSE_OFFERING_CANCELLED, IN_APP_AND_EMAIL, mailMessageResolver);
    }
}
