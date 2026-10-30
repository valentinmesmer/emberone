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
import com.emberone.courseoffering.dto.CourseOfferingDto;
import com.emberone.courseoffering.event.CourseOfferingCancelledEvent;
import com.emberone.courseoffering.exception.CourseOfferingNotFoundException;
import com.emberone.courseoffering.exception.CourseOfferingValidationException;
import com.emberone.courseoffering.repository.CourseOfferingRepository;
import com.emberone.courseoffering.repository.CourseRegistrationRepository;
import com.emberone.platform.security.TokenService;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import com.emberone.workflow.service.WorkflowService;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Cancels a published offering on behalf of the offering organization. Everything happens under the
 * offering row lock ({@link CourseOfferingRepository#findForUpdate}), the same one approvals,
 * move-ups and removals take, so a concurrent decision either lands before the cancellation and is
 * ended with it, or finds the offering cancelled and fails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseOfferingCancellationService {

    /** Registrations that still hold or wait for a place. */
    private static final List<CourseRegistrationStatus> LIVE_STATUSES =
            List.of(
                    CourseRegistrationStatus.PENDING,
                    CourseRegistrationStatus.PARTICIPANT,
                    CourseRegistrationStatus.WAITLIST);

    private final CourseOfferingRepository offeringRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final CourseOfferingService offeringService;
    private final WorkflowService workflowService;
    private final TokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final TenantTrainingCourseTypeRepository trainingCourseTypeRepository;

    /**
     * Cancels the offering: all open, participant and waitlist registrations end as cancelled by
     * the organization with the reason, their registration and cancellation workflows are closed,
     * and everyone concerned is notified once the transaction has committed. Only published
     * offerings can be cancelled; drafts are deleted, completed offerings are final.
     */
    @Transactional
    public CourseOfferingDto cancel(Long organizationId, Long offeringId, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new CourseOfferingValidationException("A reason is required");
        }
        Long tenantId = tokenService.requireTenantId();
        CourseOffering offering =
                offeringRepository
                        .findForUpdate(offeringId)
                        .filter(found -> found.getOrganizationId().equals(organizationId))
                        .orElseThrow(() -> new CourseOfferingNotFoundException(offeringId));
        if (offering.getStatus() != CourseOfferingStatus.PUBLISHED) {
            throw new CourseOfferingValidationException(
                    "Only published offerings can be cancelled, not " + offering.getStatus());
        }

        Instant now = Instant.now();
        String trimmedReason = reason.trim();
        Long actorUserId = tokenService.getReferencedUserId();
        Set<Long> userIds = new LinkedHashSet<>();
        Set<Long> workflowIds = new LinkedHashSet<>();
        for (CourseRegistration registration :
                registrationRepository.findByOffering_IdAndStatusIn(offeringId, LIVE_STATUSES)) {
            collectOpenWorkflows(registration, workflowIds);
            registration.setStatus(CourseRegistrationStatus.CANCELLED);
            registration.setEndedAt(now);
            registration.setEndedBy(CourseRegistrationEndedBy.ORGANIZATION);
            registration.setEndedByUserId(actorUserId);
            registration.setEndReason(trimmedReason);
            registrationRepository.save(registration);
            userIds.add(registration.getUserId());
        }
        workflowService.cancelActiveWorkflows(workflowIds);

        offering.setStatus(CourseOfferingStatus.CANCELLED);
        offering.setCancelledAt(now);
        offering.setCancelReason(trimmedReason);
        offeringRepository.saveAndFlush(offering);

        if (!userIds.isEmpty()) {
            eventPublisher.publishEvent(
                    new CourseOfferingCancelledEvent(
                            tenantId,
                            offeringId,
                            trainingCourseTypeName(offering),
                            trimmedReason,
                            userIds));
        }
        log.info(
                "Course offering {} cancelled; {} registration(s) ended, {} workflow(s) closed",
                offeringId,
                userIds.size(),
                workflowIds.size());
        return offeringService.get(organizationId, offeringId);
    }

    private String trainingCourseTypeName(CourseOffering offering) {
        return trainingCourseTypeRepository
                .findById(offering.getTrainingCourseTypeId())
                .filter(type -> offering.getTenantId().equals(type.getTenantId()))
                .map(TenantTrainingCourseType::getName)
                .orElse("");
    }

    /**
     * The workflows that may still be open for the registration: its registration workflow while
     * pending and a cancellation the user opened while participating. The engine ignores the ones
     * that already ended.
     */
    private static void collectOpenWorkflows(
            CourseRegistration registration, Set<Long> workflowIds) {
        if (registration.getStatus() == CourseRegistrationStatus.PENDING
                && registration.getWorkflowId() != null) {
            workflowIds.add(registration.getWorkflowId());
        }
        if (registration.getCancellationWorkflowId() != null) {
            workflowIds.add(registration.getCancellationWorkflowId());
        }
    }
}
