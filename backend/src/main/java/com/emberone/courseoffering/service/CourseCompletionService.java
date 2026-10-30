/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.domain.CourseOfferingStatus;
import com.emberone.courseoffering.domain.CourseRegistration;
import com.emberone.courseoffering.domain.CourseRegistrationEndedBy;
import com.emberone.courseoffering.domain.CourseRegistrationStatus;
import com.emberone.courseoffering.domain.CourseSession;
import com.emberone.courseoffering.dto.CourseCompletionDto;
import com.emberone.courseoffering.dto.CourseRegistrationDto;
import com.emberone.courseoffering.exception.CourseOfferingNotFoundException;
import com.emberone.courseoffering.exception.CourseOfferingValidationException;
import com.emberone.courseoffering.exception.CourseRegistrationNotFoundException;
import com.emberone.courseoffering.repository.CourseOfferingRepository;
import com.emberone.courseoffering.repository.CourseRegistrationRepository;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.time.OrganizationWallClockTime;
import com.emberone.trainingcourse.service.TrainingCourseRecordService;
import com.emberone.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Completion of a published offering: the organization sets passed or failed per participant
 * manually; confirming the list completes the offering and credits the course to everyone marked
 * passed. Setting results and completing both require the first session calendar day in the
 * organization zone.
 *
 * <p>Attendance and exam results are not part of this flow yet (extensible later). Deciding and
 * confirming take the same row lock on the offering as every other change to its registrations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseCompletionService {

    /** Registrations that still wait for a place when the offering completes. */
    private static final List<CourseRegistrationStatus> OPEN_STATUSES =
            List.of(CourseRegistrationStatus.PENDING, CourseRegistrationStatus.WAITLIST);

    static final String COMPLETED_END_REASON = "Offering completed";

    private final CourseOfferingRepository offeringRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final CourseRegistrationAssembler assembler;
    private final TrainingCourseRecordService trainingCourseRecordService;
    private final WorkflowService workflowService;
    private final TokenService tokenService;

    @Transactional(readOnly = true)
    public CourseCompletionDto get(Long organizationId, Long offeringId) {
        Long tenantId = tokenService.requireTenantId();
        CourseOffering offering =
                offeringRepository
                        .findByIdAndOrganizationId(offeringId, organizationId)
                        .orElseThrow(() -> new CourseOfferingNotFoundException(offeringId));
        return toDto(tenantId, offering);
    }

    /** Sets or clears ({@code passed} null) the final result of a participant. */
    @Transactional
    public CourseCompletionDto setFinalResult(
            Long organizationId, Long offeringId, Long registrationId, Boolean passed) {
        Long tenantId = tokenService.requireTenantId();
        CourseOffering offering = lockPublished(organizationId, offeringId);
        requireFirstSessionDayReached(offering);
        CourseRegistration registration =
                registrationRepository
                        .findByIdAndOffering_Id(registrationId, offeringId)
                        .orElseThrow(() -> new CourseRegistrationNotFoundException(registrationId));
        if (registration.getStatus() != CourseRegistrationStatus.PARTICIPANT) {
            throw new CourseOfferingValidationException(
                    "Only participants have a final result: " + registrationId);
        }
        registration.setFinalPassed(passed);
        registration.setFinalDecidedByUserId(
                passed == null ? null : tokenService.getReferencedUserId());
        registration.setFinalDecidedAt(passed == null ? null : Instant.now());
        registrationRepository.save(registration);
        log.info("Final result of registration {} set", registrationId);
        return toDto(tenantId, offering);
    }

    /**
     * Confirms the list: credits the course to participants with {@code finalPassed = true} and
     * completes the offering, all in one transaction.
     */
    @Transactional
    public CourseCompletionDto complete(Long organizationId, Long offeringId) {
        Long tenantId = tokenService.requireTenantId();
        CourseOffering offering = lockPublished(organizationId, offeringId);
        requireFirstSessionDayReached(offering);

        List<CourseRegistration> participants = participants(offeringId);
        Instant now = Instant.now();
        var startDate =
                OrganizationWallClockTime.toOrganizationDate(offering.getFirstSessionStart());
        var endDate = OrganizationWallClockTime.toOrganizationDate(offering.getLastSessionEnd());
        if (startDate == null || endDate == null) {
            throw new CourseOfferingValidationException("The offering has no sessions");
        }
        String location =
                offering.getSessions().stream()
                        .map(CourseSession::getLocation)
                        .filter(l -> l != null && !l.isBlank())
                        .findFirst()
                        .orElse(null);
        BigDecimal hours = sessionHours(offering);
        int credited = 0;
        for (CourseRegistration registration : participants) {
            if (Boolean.TRUE.equals(registration.getFinalPassed())) {
                trainingCourseRecordService.createSubmissionFromCourseRegistration(
                        tenantId,
                        registration.getUserId(),
                        offering.getTrainingCourseTypeId(),
                        startDate,
                        endDate,
                        location,
                        offering.getContent(),
                        hours,
                        registration.getId());
                credited++;
            }
        }
        registrationRepository.saveAll(participants);
        closeOpenRegistrations(offeringId, participants, now);
        offering.setStatus(CourseOfferingStatus.COMPLETED);
        offering.setCompletedAt(now);
        offeringRepository.save(offering);
        log.info(
                "Offering {} completed: {} participants, {} credited",
                offeringId,
                participants.size(),
                credited);
        return toDto(tenantId, offering);
    }

    /** Results and completion require the first session calendar day in the organization zone. */
    static void requireFirstSessionDayReached(CourseOffering offering) {
        LocalDate firstDay =
                OrganizationWallClockTime.toOrganizationDate(offering.getFirstSessionStart());
        if (firstDay == null) {
            throw new CourseOfferingValidationException("The offering has no sessions");
        }
        if (OrganizationWallClockTime.today().isBefore(firstDay)) {
            throw new CourseOfferingValidationException(
                    "The offering has not started yet (first session day)");
        }
    }

    static BigDecimal sessionHours(CourseOffering offering) {
        long minutes =
                offering.getSessions().stream()
                        .mapToLong(
                                session ->
                                        ChronoUnit.MINUTES.between(
                                                session.getStart(), session.getEnd()))
                        .sum();
        if (minutes <= 0) {
            return null;
        }
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private void closeOpenRegistrations(
            Long offeringId, List<CourseRegistration> participants, Instant now) {
        Set<Long> workflowIds = new LinkedHashSet<>();
        Long actorUserId = tokenService.getReferencedUserId();
        for (CourseRegistration registration :
                registrationRepository.findByOffering_IdAndStatusIn(offeringId, OPEN_STATUSES)) {
            if (registration.getStatus() == CourseRegistrationStatus.PENDING
                    && registration.getWorkflowId() != null) {
                workflowIds.add(registration.getWorkflowId());
            }
            if (registration.getCancellationWorkflowId() != null) {
                workflowIds.add(registration.getCancellationWorkflowId());
            }
            registration.setStatus(CourseRegistrationStatus.CANCELLED);
            registration.setEndedAt(now);
            registration.setEndedBy(CourseRegistrationEndedBy.ORGANIZATION);
            registration.setEndedByUserId(actorUserId);
            registration.setEndReason(COMPLETED_END_REASON);
            registrationRepository.save(registration);
        }
        for (CourseRegistration participant : participants) {
            if (participant.getCancellationWorkflowId() != null) {
                workflowIds.add(participant.getCancellationWorkflowId());
                participant.setCancellationWorkflowId(null);
            }
        }
        workflowService.cancelActiveWorkflows(workflowIds);
    }

    private CourseOffering lockPublished(Long organizationId, Long offeringId) {
        CourseOffering offering =
                offeringRepository
                        .findForUpdate(offeringId)
                        .filter(o -> o.getOrganizationId().equals(organizationId))
                        .orElseThrow(() -> new CourseOfferingNotFoundException(offeringId));
        if (offering.getStatus() != CourseOfferingStatus.PUBLISHED) {
            throw new CourseOfferingValidationException(
                    "The completion can only be changed while the offering is published");
        }
        return offering;
    }

    private List<CourseRegistration> participants(Long offeringId) {
        return registrationRepository.findByOffering_IdAndStatusOrderByApprovedAtAscIdAsc(
                offeringId, CourseRegistrationStatus.PARTICIPANT);
    }

    private CourseCompletionDto toDto(Long tenantId, CourseOffering offering) {
        List<CourseRegistration> participants = participants(offering.getId());
        Map<Long, CourseRegistrationDto> named = new HashMap<>();
        assembler
                .toDtos(offering.getId(), participants)
                .forEach(dto -> named.put(dto.getId(), dto));
        return CourseCompletionDto.builder()
                .offeringId(offering.getId())
                .status(offering.getStatus())
                .completedAt(offering.getCompletedAt())
                .participants(
                        participants.stream()
                                .map(
                                        registration ->
                                                CourseCompletionDto.Participant.builder()
                                                        .registrationId(registration.getId())
                                                        .userId(registration.getUserId())
                                                        .userDisplayName(
                                                                named.get(registration.getId())
                                                                        .getUserDisplayName())
                                                        .finalPassed(registration.getFinalPassed())
                                                        .decidedByUserId(
                                                                registration
                                                                        .getFinalDecidedByUserId())
                                                        .decidedAt(registration.getFinalDecidedAt())
                                                        .build())
                                .toList())
                .build();
    }
}
