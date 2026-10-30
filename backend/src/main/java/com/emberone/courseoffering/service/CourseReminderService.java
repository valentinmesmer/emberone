/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import com.emberone.courseoffering.config.CourseOfferingProperties;
import com.emberone.courseoffering.repository.CourseOfferingRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Finds published offerings due for a course-start reminder and hands each one to {@link
 * CourseReminderProcessor}. A failing offering is logged and does not keep the others from being
 * reminded; it is tried again by the next sweep because its marker was not stored. Meant to be
 * invoked once per tenant by a scheduled batch.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseReminderService {

    private final CourseOfferingRepository offeringRepository;
    private final CourseReminderProcessor processor;
    private final CourseOfferingProperties properties;

    /**
     * Sends course-start reminders that are due for the tenant. The reminder writes run in their
     * own transactions in the processor.
     */
    public void sendRemindersForTenant(Long tenantId) {
        Instant now = Instant.now();
        List<Long> startIds =
                offeringRepository.findIdsDueForStartReminder(
                        now, now.plus(properties.getStartReminderLead()));
        int sent = 0;
        for (Long offeringId : startIds) {
            try {
                sent += processor.remindCourseStart(tenantId, offeringId) ? 1 : 0;
            } catch (RuntimeException e) {
                log.error("Start reminder failed for course offering {}", offeringId, e);
            }
        }
        if (sent > 0) {
            log.info("Course offering reminder sweep reminded for {} offering(s)", sent);
        }
    }
}
