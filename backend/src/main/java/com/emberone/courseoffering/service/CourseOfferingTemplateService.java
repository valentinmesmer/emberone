/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.domain.CourseOfferingAudience;
import com.emberone.courseoffering.domain.CourseOfferingPrerequisite;
import com.emberone.courseoffering.domain.CourseOfferingStatus;
import com.emberone.courseoffering.domain.CourseSession;
import com.emberone.courseoffering.dto.CourseOfferingDto;
import com.emberone.courseoffering.exception.CourseOfferingValidationException;
import com.emberone.courseoffering.repository.CourseOfferingRepository;
import com.emberone.platform.time.OrganizationWallClockTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Templates are offerings in status {@code TEMPLATE}: same tables, same editing. This service only
 * copies between the two worlds; a copy keeps no link to its source.
 */
@Service
@RequiredArgsConstructor
public class CourseOfferingTemplateService {

    private final CourseOfferingRepository offeringRepository;
    private final CourseOfferingCatalogService catalogService;
    private final CourseOfferingService offeringService;

    /** Copies any offering (as is, unshifted) into a new template of the same organization. */
    @Transactional
    public CourseOfferingDto saveAsTemplate(
            Long organizationId, Long offeringId, String templateName) {
        CourseOffering source = offeringService.requireOffering(organizationId, offeringId);
        if (source.getStatus() == CourseOfferingStatus.TEMPLATE) {
            throw new CourseOfferingValidationException("A template cannot be copied as template");
        }
        CourseOffering template =
                copy(source, CourseOfferingStatus.TEMPLATE, templateName.trim(), 0L);
        template.syncSchedule();
        return offeringService.get(
                organizationId, offeringRepository.saveAndFlush(template).getId());
    }

    /**
     * Creates a draft from a template. All sessions and the registration period move by the same
     * number of calendar days so that the first session lands on {@code firstSessionDate}; local
     * times and the gaps between sessions are kept, also across daylight saving changes.
     */
    @Transactional
    public CourseOfferingDto instantiate(
            Long organizationId, Long templateId, LocalDate firstSessionDate) {
        CourseOffering template = offeringService.requireOffering(organizationId, templateId);
        if (template.getStatus() != CourseOfferingStatus.TEMPLATE) {
            throw new CourseOfferingValidationException("Only templates can be instantiated");
        }
        catalogService.requireOfferable(template.getTrainingCourseTypeId(), organizationId);
        long shiftDays = shiftDays(template, firstSessionDate);
        CourseOffering draft = copy(template, CourseOfferingStatus.DRAFT, null, shiftDays);
        draft.syncSchedule();
        return offeringService.get(organizationId, offeringRepository.saveAndFlush(draft).getId());
    }

    private static long shiftDays(CourseOffering template, LocalDate firstSessionDate) {
        if (template.getSessions().isEmpty()) {
            return 0L;
        }
        ZoneId zone = OrganizationWallClockTime.organizationZone();
        Instant templateStart =
                template.getSessions().stream()
                        .map(CourseSession::getStart)
                        .min(Instant::compareTo)
                        .orElseThrow();
        return ChronoUnit.DAYS.between(templateStart.atZone(zone).toLocalDate(), firstSessionDate);
    }

    private static Instant shift(Instant instant, long days) {
        if (instant == null || days == 0) {
            return instant;
        }
        ZoneId zone = OrganizationWallClockTime.organizationZone();
        return instant.atZone(zone).plusDays(days).toInstant();
    }

    private CourseOffering copy(
            CourseOffering source, CourseOfferingStatus status, String templateName, long days) {
        CourseOffering copy =
                CourseOffering.builder()
                        .tenantId(source.getTenantId())
                        .organizationId(source.getOrganizationId())
                        .trainingCourseTypeId(source.getTrainingCourseTypeId())
                        .content(source.getContent())
                        .notes(source.getNotes())
                        .templateName(templateName)
                        .status(status)
                        .registrationStart(shift(source.getRegistrationStart(), days))
                        .registrationEnd(shift(source.getRegistrationEnd(), days))
                        .maxParticipants(source.getMaxParticipants())
                        .hasExam(source.isHasExam())
                        .requiresHomeOrgApproval(source.isRequiresHomeOrgApproval())
                        .trainingCourseRequirementMode(source.getTrainingCourseRequirementMode())
                        .certRequirementMode(source.getCertRequirementMode())
                        .drivingLicenseRequirementMode(source.getDrivingLicenseRequirementMode())
                        .sessions(new ArrayList<>())
                        .audiences(new ArrayList<>())
                        .prerequisites(new ArrayList<>())
                        .build();
        for (CourseSession session : source.getSessions()) {
            copy.getSessions()
                    .add(
                            CourseSession.builder()
                                    .tenantId(source.getTenantId())
                                    .offering(copy)
                                    .type(session.getType())
                                    .start(shift(session.getStart(), days))
                                    .end(shift(session.getEnd(), days))
                                    .location(session.getLocation())
                                    .notes(session.getNotes())
                                    .build());
        }
        for (CourseOfferingAudience audience : source.getAudiences()) {
            copy.getAudiences()
                    .add(
                            CourseOfferingAudience.builder()
                                    .tenantId(source.getTenantId())
                                    .offering(copy)
                                    .kind(audience.getKind())
                                    .refId(audience.getRefId())
                                    .quota(audience.getQuota())
                                    .build());
        }
        for (CourseOfferingPrerequisite prerequisite : source.getPrerequisites()) {
            copy.getPrerequisites()
                    .add(
                            CourseOfferingPrerequisite.builder()
                                    .tenantId(source.getTenantId())
                                    .offering(copy)
                                    .kind(prerequisite.getKind())
                                    .configKey(prerequisite.getConfigKey())
                                    .build());
        }
        return copy;
    }
}
