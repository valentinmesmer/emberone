/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.domain.CourseOfferingStatus;
import com.emberone.courseoffering.domain.CourseRegistrationStatus;
import com.emberone.courseoffering.repository.CourseOfferingRepository;
import com.emberone.courseoffering.repository.CourseRegistrationRepository;
import com.emberone.notification.domain.NotificationParamKeys;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.service.NotificationRequest;
import com.emberone.notification.service.NotificationService;
import com.emberone.platform.time.OrganizationWallClockTime;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Sends the start reminder of one offering. Recipient resolution runs without the offering row
 * lock; only the idempotent marker is written in a short separate transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseReminderProcessor {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm 'Uhr'");

    private final CourseOfferingRepository offeringRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final NotificationService notificationService;
    private final PlatformTransactionManager transactionManager;
    private final TenantTrainingCourseTypeRepository trainingCourseTypeRepository;

    /**
     * Reminds participants and waitlisted users that the first session is near. The marker is only
     * stored once someone was reminded, so participants approved after an early sweep still get it.
     *
     * @return whether anyone was reminded
     */
    @Transactional(readOnly = true)
    public boolean remindCourseStart(Long tenantId, Long offeringId) {
        Instant now = Instant.now();
        CourseOffering offering = offeringRepository.findById(offeringId).orElse(null);
        if (!startReminderDue(offering, now)) {
            return false;
        }
        Set<Long> userIds =
                new LinkedHashSet<>(
                        registrationRepository.findUserIdsByOfferingAndStatusIn(
                                offeringId,
                                List.of(
                                        CourseRegistrationStatus.PARTICIPANT,
                                        CourseRegistrationStatus.WAITLIST)));
        if (userIds.isEmpty()) {
            return false;
        }
        CourseOffering snapshot = offering;
        return Boolean.TRUE.equals(
                requiresNewTemplate()
                        .execute(
                                status -> {
                                    CourseOffering locked =
                                            offeringRepository
                                                    .findForUpdate(offeringId)
                                                    .orElse(null);
                                    if (!startReminderDue(locked, now)) {
                                        return false;
                                    }
                                    locked.setStartReminderSentAt(now);
                                    offeringRepository.save(locked);
                                    notify(
                                            tenantId,
                                            userIds,
                                            NotificationType.COURSE_OFFERING_START_REMINDER,
                                            Map.of(
                                                    NotificationParamKeys.COURSE_OFFERING_TOPIC,
                                                    trainingCourseTypeName(snapshot),
                                                    NotificationParamKeys.COURSE_OFFERING_START,
                                                    format(snapshot.getFirstSessionStart())));
                                    return true;
                                }));
    }

    private TransactionTemplate requiresNewTemplate() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    private static boolean startReminderDue(CourseOffering offering, Instant now) {
        return offering != null
                && offering.getStatus() == CourseOfferingStatus.PUBLISHED
                && offering.getStartReminderSentAt() == null
                && offering.getFirstSessionStart() != null
                && offering.getFirstSessionStart().isAfter(now);
    }

    private void notify(
            Long tenantId, Set<Long> userIds, NotificationType type, Map<String, Object> params) {
        if (userIds.isEmpty()) {
            return;
        }
        notificationService.notify(
                NotificationRequest.builder()
                        .tenantId(tenantId)
                        .recipientUserIds(userIds)
                        .type(type)
                        .params(params)
                        .build());
    }

    private static String format(Instant instant) {
        return DISPLAY_FORMAT.format(instant.atZone(OrganizationWallClockTime.organizationZone()));
    }

    private String trainingCourseTypeName(CourseOffering offering) {
        return trainingCourseTypeRepository
                .findById(offering.getTrainingCourseTypeId())
                .filter(type -> offering.getTenantId().equals(type.getTenantId()))
                .map(TenantTrainingCourseType::getName)
                .orElse("");
    }
}
