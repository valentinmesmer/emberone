/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.domain.CourseOfferingAudience;
import com.emberone.courseoffering.domain.CourseOfferingStatus;
import com.emberone.courseoffering.domain.CourseRegistration;
import com.emberone.courseoffering.domain.CourseRegistrationEndedBy;
import com.emberone.courseoffering.domain.CourseRegistrationStatus;
import com.emberone.courseoffering.dto.CourseRegistrationDto;
import com.emberone.courseoffering.event.CourseRegistrationRemovedEvent;
import com.emberone.courseoffering.exception.CourseOfferingNotFoundException;
import com.emberone.courseoffering.exception.CourseOfferingValidationException;
import com.emberone.courseoffering.exception.CourseRegistrationNotFoundException;
import com.emberone.courseoffering.list.CourseOfferingListSpecs;
import com.emberone.courseoffering.repository.CourseOfferingRepository;
import com.emberone.courseoffering.repository.CourseRegistrationRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.security.TokenService;
import com.emberone.trainingcourse.repository.TrainingCourseUserSubmissionRepository;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Registrations of users for offerings: the rules for opening one, and every change that moves
 * seats (approval, move-up from the waitlist, removal).
 *
 * <p>Seat usage is decided only under a pessimistic write lock on the offering row ({@link
 * CourseOfferingRepository#findForUpdate}). Approval, move-up and removal all take it before
 * counting, so concurrent calls for the same offering run one after another and can never overbook
 * it. Seats are counted against the offering's overall maximum and, for offerings with quotas,
 * additionally against the quota of the audience entry the registration belongs to (see {@link
 * #place}). Every registration holding a seat or a waitlist place of such an offering stores that
 * entry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseRegistrationService {

    private final CourseOfferingRepository offeringRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final CourseAudienceScopeService scopeService;
    private final CoursePrerequisiteService prerequisiteService;
    private final TrainingCourseUserSubmissionRepository submissionRepository;
    private final CourseRegistrationAssembler assembler;
    private final TokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;
    private final TenantTrainingCourseTypeRepository trainingCourseTypeRepository;

    /** An offering the user may register for right now. */
    public record Candidate(CourseOffering offering, boolean alreadyHeld) {}

    /**
     * Checks that the user may open a registration: the offering is published, the registration
     * window is open, the user is eligible through an active membership, has no active registration
     * yet and meets the prerequisites.
     */
    @Transactional(readOnly = true)
    public Candidate requireRegistrable(Long userId, Long offeringId) {
        CourseOffering offering =
                offeringRepository
                        .findOne(
                                Specification.allOf(
                                        CourseOfferingListSpecs.published(),
                                        CourseOfferingListSpecs.withId(offeringId),
                                        CourseOfferingListSpecs.eligibleFor(
                                                scopeService.forUser(userId))))
                        .orElseThrow(() -> new CourseOfferingNotFoundException(offeringId));
        if (!offering.isRegistrationOpen(Instant.now())) {
            throw new CourseOfferingValidationException(
                    "Registration is not open for this offering");
        }
        if (registrationRepository.existsByOffering_IdAndUserIdAndStatusIn(
                offeringId,
                userId,
                List.of(
                        CourseRegistrationStatus.PENDING,
                        CourseRegistrationStatus.PARTICIPANT,
                        CourseRegistrationStatus.WAITLIST))) {
            throw new CourseOfferingValidationException(
                    "You already have an active registration for this offering");
        }
        if (!prerequisiteService.check(offering, userId).isMet()) {
            throw new CourseOfferingValidationException(
                    "The prerequisites of this offering are not met");
        }
        boolean alreadyHeld =
                submissionRepository
                        .findGrantedTypeIdsByUser(userId)
                        .contains(offering.getTrainingCourseTypeId());
        return new Candidate(offering, alreadyHeld);
    }

    /** Records the registration when its workflow opens. */
    @Transactional
    public CourseRegistration createPending(
            Long tenantId, Long userId, Long offeringId, Long workflowId, boolean alreadyHeld) {
        return registrationRepository.save(
                CourseRegistration.builder()
                        .tenantId(tenantId)
                        .offering(offeringRepository.getReferenceById(offeringId))
                        .userId(userId)
                        .workflowId(workflowId)
                        .status(CourseRegistrationStatus.PENDING)
                        .alreadyHeld(alreadyHeld)
                        .registeredAt(Instant.now())
                        .build());
    }

    /**
     * Workflow approved: a free seat makes the user a participant, otherwise the registration joins
     * the waitlist, ordered by this approval time.
     */
    @Transactional
    public CourseRegistration approve(Long workflowId) {
        CourseRegistration registration =
                registrationRepository
                        .findByWorkflowId(workflowId)
                        .orElseThrow(
                                () ->
                                        new CourseOfferingValidationException(
                                                "No course registration for workflow "
                                                        + workflowId));
        CourseOffering offering = lockOffering(registration.getOffering().getId());
        refreshUnderLock(registration);
        if (registration.getStatus() != CourseRegistrationStatus.PENDING) {
            throw new CourseOfferingValidationException("The registration is no longer pending");
        }
        if (offering.getStatus() != CourseOfferingStatus.PUBLISHED) {
            throw new CourseOfferingValidationException(
                    "The offering no longer accepts registrations");
        }
        Placement placement = place(offering, registration.getUserId());
        registration.setApprovedAt(Instant.now());
        registration.setStatus(placement.status());
        registration.setAudienceId(placement.audienceId());
        log.info(
                "Course registration {} approved as {}",
                registration.getId(),
                registration.getStatus());
        return registrationRepository.save(registration);
    }

    /** Workflow rejected: the registration ends. */
    @Transactional
    public void reject(Long workflowId) {
        endPending(workflowId, CourseRegistrationStatus.REJECTED, null);
    }

    /** Workflow cancelled by the user: the registration ends. */
    @Transactional
    public void cancelByUser(Long workflowId) {
        endPending(workflowId, CourseRegistrationStatus.CANCELLED, CourseRegistrationEndedBy.USER);
    }

    /**
     * Moves a waitlist registration up to participant. Manual, chosen by the organization; refused
     * while no seat is free.
     */
    @Transactional
    public CourseRegistrationDto promote(
            Long organizationId, Long offeringId, Long registrationId) {
        CourseOffering offering = lockOffering(organizationId, offeringId);
        requireChangeable(offering);
        CourseRegistration registration = requireRegistration(offeringId, registrationId);
        if (registration.getStatus() != CourseRegistrationStatus.WAITLIST) {
            throw new CourseOfferingValidationException(
                    "Only waitlist registrations can be moved up");
        }
        if (!hasFreeSeat(offering)) {
            throw new ConflictException("The offering has no free seat");
        }
        if (isQuotaOffering(offering) && !hasFreeQuotaSeat(offering, registration)) {
            throw new ConflictException("The quota of the audience entry has no free seat");
        }
        registration.setStatus(CourseRegistrationStatus.PARTICIPANT);
        registrationRepository.save(registration);
        log.info("Course registration {} moved up from the waitlist", registrationId);
        return assembler.toDtos(offeringId, List.of(registration)).getFirst();
    }

    /**
     * Ends a participant or waitlist place on behalf of the organization. The reason is mandatory
     * and sent to the user; the seat becomes free. Open registrations are decided in their workflow
     * instead.
     */
    @Transactional
    public CourseRegistrationDto remove(
            Long organizationId, Long offeringId, Long registrationId, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new CourseOfferingValidationException("A reason is required");
        }
        Long tenantId = tokenService.requireTenantId();
        CourseOffering offering = lockOffering(organizationId, offeringId);
        requireChangeable(offering);
        CourseRegistration registration = requireRegistration(offeringId, registrationId);
        if (registration.getStatus() != CourseRegistrationStatus.PARTICIPANT
                && registration.getStatus() != CourseRegistrationStatus.WAITLIST) {
            throw new CourseOfferingValidationException(
                    "Only participants and waitlist places can be removed; decide open"
                            + " registrations in their workflow");
        }
        registration.setStatus(CourseRegistrationStatus.CANCELLED);
        registration.setEndedAt(Instant.now());
        registration.setEndedBy(CourseRegistrationEndedBy.ORGANIZATION);
        registration.setEndedByUserId(tokenService.getReferencedUserId());
        registration.setEndReason(reason.trim());
        registrationRepository.save(registration);
        eventPublisher.publishEvent(
                new CourseRegistrationRemovedEvent(
                        tenantId,
                        registration.getUserId(),
                        offeringId,
                        trainingCourseTypeName(offering),
                        registration.getEndReason()));
        log.info("Course registration {} removed by the organization", registrationId);
        return assembler.toDtos(offeringId, List.of(registration)).getFirst();
    }

    // --- cancellation by the user (workflow) and waitlist withdrawal ---

    /** A participant's registration that may be cancelled right now, with its offering. */
    public record CancellationCandidate(CourseRegistration registration, CourseOffering offering) {}

    /**
     * Checks that the user may start a cancellation workflow: the registration is the user's own
     * and a participant place (open registrations are cancelled through their workflow, waitlist
     * places are withdrawn without one), no cancellation is open yet, the offering is still
     * published and its first session has not started.
     */
    @Transactional(readOnly = true)
    public CancellationCandidate requireCancellable(Long userId, Long registrationId) {
        CourseRegistration registration =
                registrationRepository
                        .findByIdAndUserId(registrationId, userId)
                        .orElseThrow(() -> new CourseRegistrationNotFoundException(registrationId));
        CourseOffering offering = registration.getOffering();
        switch (registration.getStatus()) {
            case PARTICIPANT -> {}
            case PENDING ->
                    throw new CourseOfferingValidationException(
                            "An open registration is cancelled by cancelling its workflow");
            case WAITLIST ->
                    throw new CourseOfferingValidationException(
                            "A waitlist place is withdrawn directly, without a cancellation");
            default ->
                    throw new CourseOfferingValidationException(
                            "The registration has already ended");
        }
        if (registration.getCancellationWorkflowId() != null) {
            throw new CourseOfferingValidationException(
                    "A cancellation of this registration is already open");
        }
        if (!isBeforeFirstSession(offering, Instant.now())) {
            throw new CourseOfferingValidationException(
                    "The offering has started; cancelling is no longer possible");
        }
        if (offering.getStatus() != CourseOfferingStatus.PUBLISHED) {
            throw new CourseOfferingValidationException(
                    "Registrations can only be changed while the offering is published");
        }
        return new CancellationCandidate(registration, offering);
    }

    /** Whether the user could cancel this registration now; same rules as the workflow check. */
    static boolean canCancel(CourseRegistration registration, Instant now) {
        CourseOffering offering = registration.getOffering();
        return registration.getStatus() == CourseRegistrationStatus.PARTICIPANT
                && registration.getCancellationWorkflowId() == null
                && offering.getStatus() == CourseOfferingStatus.PUBLISHED
                && isBeforeFirstSession(offering, now);
    }

    /**
     * Links the opened cancellation workflow to the registration. Under the offering lock the
     * registration must still be a participant without another cancellation, which excludes a
     * parallel removal or second cancellation.
     */
    @Transactional
    public void cancellationOpened(Long userId, Long registrationId, Long workflowId) {
        Long offeringId =
                registrationRepository
                        .findOfferingIdByIdAndUser(registrationId, userId)
                        .orElseThrow(() -> new CourseRegistrationNotFoundException(registrationId));
        lockOffering(offeringId);
        CourseRegistration registration =
                registrationRepository
                        .findByIdAndUserId(registrationId, userId)
                        .orElseThrow(() -> new CourseRegistrationNotFoundException(registrationId));
        refreshUnderLock(registration);
        if (registration.getStatus() != CourseRegistrationStatus.PARTICIPANT
                || registration.getCancellationWorkflowId() != null) {
            throw new CourseOfferingValidationException(
                    "The registration can no longer be cancelled");
        }
        registration.setCancellationWorkflowId(workflowId);
        registrationRepository.save(registration);
    }

    /**
     * Cancellation approved by all deciders: the registration ends as cancelled by the user and the
     * seat becomes free. A registration the organization already ended meanwhile stays as it is.
     * Fails when the offering no longer accepts changes (the deciders reject then).
     */
    @Transactional
    public void approveCancellation(Long workflowId) {
        Long offeringId =
                registrationRepository
                        .findOfferingIdByCancellationWorkflow(workflowId)
                        .orElse(null);
        if (offeringId == null) {
            log.warn("No course registration for cancellation workflow {}", workflowId);
            return;
        }
        CourseOffering offering = lockOffering(offeringId);
        CourseRegistration registration =
                registrationRepository
                        .findByCancellationWorkflowId(workflowId)
                        .orElseThrow(
                                () ->
                                        new CourseOfferingValidationException(
                                                "No course registration for workflow "
                                                        + workflowId));
        refreshUnderLock(registration);
        if (registration.getStatus() != CourseRegistrationStatus.PARTICIPANT) {
            log.info(
                    "Course registration {} already ended as {}; cancellation has no effect",
                    registration.getId(),
                    registration.getStatus());
            return;
        }
        requireChangeable(offering);
        endByUser(registration);
        log.info("Course registration {} cancelled by the user", registration.getId());
    }

    /**
     * Cancellation rejected or aborted: the registration stays as it is and may be cancelled again.
     */
    @Transactional
    public void cancellationDeclined(Long workflowId) {
        Long offeringId =
                registrationRepository
                        .findOfferingIdByCancellationWorkflow(workflowId)
                        .orElse(null);
        if (offeringId == null) {
            return;
        }
        lockOffering(offeringId);
        registrationRepository
                .findByCancellationWorkflowId(workflowId)
                .ifPresent(
                        registration -> {
                            registration.setCancellationWorkflowId(null);
                            registrationRepository.save(registration);
                        });
    }

    /**
     * The user leaves the waitlist: no workflow, since no seat is involved. Only the user's own
     * waitlist place qualifies.
     */
    @Transactional
    public void withdrawFromWaitlist(Long userId, Long registrationId) {
        Long offeringId =
                registrationRepository
                        .findOfferingIdByIdAndUser(registrationId, userId)
                        .orElseThrow(() -> new CourseRegistrationNotFoundException(registrationId));
        lockOffering(offeringId);
        CourseRegistration registration =
                registrationRepository
                        .findByIdAndUserId(registrationId, userId)
                        .orElseThrow(() -> new CourseRegistrationNotFoundException(registrationId));
        if (registration.getStatus() != CourseRegistrationStatus.WAITLIST) {
            throw new CourseOfferingValidationException(
                    "Only a waitlist place can be withdrawn without a workflow");
        }
        endByUser(registration);
        log.info("Course registration {} withdrawn from the waitlist", registrationId);
    }

    private void endByUser(CourseRegistration registration) {
        registration.setStatus(CourseRegistrationStatus.CANCELLED);
        registration.setEndedAt(Instant.now());
        registration.setEndedBy(CourseRegistrationEndedBy.USER);
        registrationRepository.save(registration);
    }

    /**
     * Re-reads a registration once the offering lock is held. The same transaction may have loaded
     * it before the lock (the workflow's {@code validateOpen} or a decision that finishes right at
     * opening), and Hibernate would otherwise hand back that cached instance: its checks would see
     * a state that a removal or cancellation committed meanwhile has replaced, and the next flush
     * would write the old status back over it.
     */
    private void refreshUnderLock(CourseRegistration registration) {
        entityManager.refresh(registration);
    }

    private static boolean isBeforeFirstSession(CourseOffering offering, Instant now) {
        return offering.getFirstSessionStart() != null
                && now.isBefore(offering.getFirstSessionStart());
    }

    /**
     * Whether another participant fits under the overall maximum; call only while holding the
     * offering lock. The quota of an audience entry is checked on top of this.
     */
    private boolean hasFreeSeat(CourseOffering offering) {
        long participants =
                registrationRepository.countByOffering_IdAndStatus(
                        offering.getId(), CourseRegistrationStatus.PARTICIPANT);
        return participants < offering.getMaxParticipants();
    }

    /** Where an approved registration lands: the status and, with quotas, the audience entry. */
    private record Placement(CourseRegistrationStatus status, Long audienceId) {}

    /**
     * Decides seat or waitlist for a newly approved registration; call only while holding the
     * offering lock. Without quotas only the overall maximum counts. With quotas the user takes the
     * most specific matching entry that still has a free quota seat (and only while the overall
     * maximum is not reached); if none has, the user joins the waitlist of the most specific
     * matching entry. The match uses the user's current memberships and the current organization
     * tree.
     */
    private Placement place(CourseOffering offering, Long userId) {
        if (!isQuotaOffering(offering)) {
            return new Placement(
                    hasFreeSeat(offering)
                            ? CourseRegistrationStatus.PARTICIPANT
                            : CourseRegistrationStatus.WAITLIST,
                    null);
        }
        List<CourseOfferingAudience> matching =
                CourseAudienceMatcher.matching(offering, scopeService.forUser(userId));
        if (matching.isEmpty()) {
            throw new CourseOfferingValidationException(
                    "The user no longer belongs to any audience entry of this offering");
        }
        if (hasFreeSeat(offering)) {
            Optional<CourseOfferingAudience> entry =
                    CourseAudienceMatcher.firstWithFreeSeat(
                            matching, participantsByAudience(offering));
            if (entry.isPresent()) {
                return new Placement(CourseRegistrationStatus.PARTICIPANT, entry.get().getId());
            }
        }
        return new Placement(CourseRegistrationStatus.WAITLIST, matching.getFirst().getId());
    }

    private static boolean isQuotaOffering(CourseOffering offering) {
        return offering.getAudiences().stream().anyMatch(entry -> entry.getQuota() != null);
    }

    /** Whether the quota of the registration's own audience entry has a free seat. */
    private boolean hasFreeQuotaSeat(CourseOffering offering, CourseRegistration registration) {
        CourseOfferingAudience entry =
                offering.getAudiences().stream()
                        .filter(e -> e.getId().equals(registration.getAudienceId()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new CourseOfferingValidationException(
                                                "The registration belongs to no audience entry"
                                                        + " of this offering"));
        return participantsByAudience(offering).getOrDefault(entry.getId(), 0L) < entry.getQuota();
    }

    private Map<Long, Long> participantsByAudience(CourseOffering offering) {
        Map<Long, Long> taken = new HashMap<>();
        registrationRepository.countSeatsByAudience(offering.getId()).stream()
                .filter(row -> row.getStatus() == CourseRegistrationStatus.PARTICIPANT)
                .forEach(row -> taken.put(row.getAudienceId(), row.getCount()));
        return taken;
    }

    private void endPending(
            Long workflowId, CourseRegistrationStatus status, CourseRegistrationEndedBy endedBy) {
        registrationRepository
                .findByWorkflowId(workflowId)
                .filter(r -> r.getStatus() == CourseRegistrationStatus.PENDING)
                .ifPresent(
                        registration -> {
                            registration.setStatus(status);
                            registration.setEndedAt(Instant.now());
                            registration.setEndedBy(endedBy);
                            registrationRepository.save(registration);
                            log.info(
                                    "Course registration {} ended as {}",
                                    registration.getId(),
                                    status);
                        });
    }

    private CourseOffering lockOffering(Long organizationId, Long offeringId) {
        CourseOffering offering = lockOffering(offeringId);
        if (!offering.getOrganizationId().equals(organizationId)) {
            throw new CourseOfferingNotFoundException(offeringId);
        }
        return offering;
    }

    private CourseOffering lockOffering(Long offeringId) {
        return offeringRepository
                .findForUpdate(offeringId)
                .orElseThrow(() -> new CourseOfferingNotFoundException(offeringId));
    }

    private static void requireChangeable(CourseOffering offering) {
        if (offering.getStatus() != CourseOfferingStatus.PUBLISHED) {
            throw new CourseOfferingValidationException(
                    "Registrations can only be changed while the offering is published");
        }
    }

    private CourseRegistration requireRegistration(Long offeringId, Long registrationId) {
        return registrationRepository
                .findByIdAndOffering_Id(registrationId, offeringId)
                .orElseThrow(() -> new CourseRegistrationNotFoundException(registrationId));
    }

    private String trainingCourseTypeName(CourseOffering offering) {
        return trainingCourseTypeRepository
                .findById(offering.getTrainingCourseTypeId())
                .filter(type -> offering.getTenantId().equals(type.getTenantId()))
                .map(TenantTrainingCourseType::getName)
                .orElse("");
    }
}
