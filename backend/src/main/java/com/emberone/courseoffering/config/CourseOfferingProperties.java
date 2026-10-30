/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "emberone.course-offerings")
public class CourseOfferingProperties {

    private String reminderCron = "0 0 7 * * *";

    /** How long before the first session participants and waitlisted users are reminded. */
    private Duration startReminderLead = Duration.ofHours(72);
}
