/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.domain.CourseOfferingAudience;
import com.emberone.courseoffering.domain.CourseRegistration;
import com.emberone.courseoffering.domain.CourseRegistrationStatus;
import com.emberone.courseoffering.domain.CourseSession;
import com.emberone.courseoffering.dto.CoursePrerequisiteCheckDto;
import com.emberone.courseoffering.dto.CourseSessionDto;
import com.emberone.courseoffering.dto.SelfCourseOfferingDto;
import com.emberone.courseoffering.dto.SelfCourseOfferingMyRegistrationDto;
import com.emberone.courseoffering.dto.SelfCourseOfferingPageDto;
import com.emberone.courseoffering.exception.CourseOfferingNotFoundException;
import com.emberone.courseoffering.list.CourseOfferingListDefinition;
import com.emberone.courseoffering.list.CourseOfferingListSpecs;
import com.emberone.courseoffering.list.SelfCourseOfferingListFilter;
import com.emberone.courseoffering.list.SelfCourseOfferingParticipationState;
import com.emberone.courseoffering.list.SelfCourseOfferingRegistrationState;
import com.emberone.courseoffering.repository.CourseOfferingRepository;
import com.emberone.courseoffering.repository.CourseRegistrationRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.PageReorder;
import com.emberone.trainingcourse.repository.TrainingCourseUserSubmissionRepository;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Offerings a user may register for, across all organizations of the tenant. */
@Service
@RequiredArgsConstructor
public class SelfCourseOfferingService {

    private final CourseOfferingRepository offeringRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final CourseAudienceScopeService scopeService;
    private final CoursePrerequisiteService prerequisiteService;
    private final TrainingCourseUserSubmissionRepository submissionRepository;
    private final TenantTrainingCourseTypeRepository trainingCourseTypeRepository;
    private final OrganizationRepository organizationRepository;
    private final TokenService tokenService;

    @Transactional(readOnly = true)
    public SelfCourseOfferingPageDto list(
            Long userId, ListQuery query, SelfCourseOfferingListFilter filter) {
        Instant now = Instant.now();
        CourseAudienceScope scope = scopeService.forUser(userId);
        Set<Long> heldTypeIds =
                new HashSet<>(submissionRepository.findGrantedTypeIdsByUser(userId));
        Specification<CourseOffering> eligible = eligibleOfferings(scope, now);
        if (!filter.includeHeld()) {
            eligible = eligible.and(CourseOfferingListSpecs.typeNotIn(heldTypeIds));
        }
        if (!filter.includeUnfulfilled()) {
            Set<Long> unmet =
                    prerequisiteService.unmetOfferingIds(
                            offeringRepository.findAll(eligible), userId);
            eligible = eligible.and(CourseOfferingListSpecs.idNotIn(unmet));
        }
        Specification<CourseOffering> base =
                Specification.allOf(
                        Specification.anyOf(
                                eligible,
                                CourseOfferingListSpecs.userHasActiveRegistration(userId)),
                        CourseOfferingListDefinition.INSTANCE.searchSpecification(query));
        Specification<CourseOffering> spec = base;
        if (!filter.registration().isEmpty()) {
            Set<Long> fullIds =
                    filter.registration().contains(SelfCourseOfferingRegistrationState.FULL)
                            ? fullOfferingIds(offeringRepository.findAll(base), scope)
                            : Set.of();
            spec = spec.and(matchingRegistration(filter.registration(), now, fullIds));
        }
        if (!filter.participation().isEmpty()) {
            spec = spec.and(matchingParticipation(filter.participation(), userId));
        }
        Pageable pageable = CourseOfferingListDefinition.INSTANCE.pageable(query);
        Page<CourseOffering> page = offeringRepository.findAll(spec, pageable);
        List<Long> ids = page.getContent().stream().map(CourseOffering::getId).toList();
        List<CourseOffering> ordered =
                PageReorder.reorderByIds(
                        ids,
                        ids.isEmpty() ? List.of() : offeringRepository.findByIdIn(ids),
                        CourseOffering::getId);
        Map<Long, String> typeNames =
                trainingCourseTypeRepository
                        .findAllById(
                                ordered.stream()
                                        .map(CourseOffering::getTrainingCourseTypeId)
                                        .distinct()
                                        .toList())
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        TenantTrainingCourseType::getId,
                                        TenantTrainingCourseType::getName));
        Map<Long, String> organizationNames =
                organizationRepository
                        .findAllById(
                                ordered.stream()
                                        .map(CourseOffering::getOrganizationId)
                                        .distinct()
                                        .toList())
                        .stream()
                        .collect(Collectors.toMap(Organization::getId, Organization::getName));
        Set<Long> quotaAudienceIds =
                ordered.stream()
                        .flatMap(offering -> offering.getAudiences().stream())
                        .filter(entry -> entry.getQuota() != null)
                        .map(CourseOfferingAudience::getId)
                        .collect(Collectors.toSet());
        Map<Long, Long> participantsByAudience =
                quotaAudienceIds.isEmpty()
                        ? Map.of()
                        : registrationRepository
                                .countParticipantsByAudience(quotaAudienceIds)
                                .stream()
                                .collect(
                                        Collectors.toMap(
                                                CourseRegistrationRepository.AudienceSeatCount
                                                        ::getAudienceId,
                                                CourseRegistrationRepository.AudienceSeatCount
                                                        ::getCount));
        Map<Long, Long> participantCounts =
                ids.isEmpty()
                        ? Map.of()
                        : registrationRepository.countParticipants(ids).stream()
                                .collect(
                                        Collectors.toMap(
                                                CourseRegistrationRepository.ParticipantCount
                                                        ::getOfferingId,
                                                CourseRegistrationRepository.ParticipantCount
                                                        ::getCount));
        Map<Long, CourseRegistration> myRegistrations = activeRegistrationsByOffering(userId, ids);
        Map<Long, Integer> waitlistPositions = waitlistPositions(myRegistrations.values());
        Set<Long> unmetOnPage = prerequisiteService.unmetOfferingIds(ordered, userId);
        return SelfCourseOfferingPageDto.builder()
                .content(
                        ordered.stream()
                                .map(
                                        offering -> {
                                            CourseRegistration myRegistration =
                                                    myRegistrations.get(offering.getId());
                                            Integer waitlistPosition =
                                                    myRegistration != null
                                                            ? waitlistPositions.get(
                                                                    myRegistration.getId())
                                                            : null;
                                            return toDto(
                                                    offering,
                                                    typeNames,
                                                    organizationNames,
                                                    heldTypeIds,
                                                    participantCounts,
                                                    participantsByAudience,
                                                    scope,
                                                    now,
                                                    myRegistration,
                                                    waitlistPosition,
                                                    !unmetOnPage.contains(offering.getId()));
                                        })
                                .toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    @Transactional(readOnly = true)
    public SelfCourseOfferingDto get(Long userId, Long offeringId) {
        Instant now = Instant.now();
        CourseOffering offering = resolveVisibleOffering(userId, offeringId, now);
        return toSingleDto(userId, offering, now);
    }

    /** Prerequisite status of the caller for an offering the caller may see. */
    @Transactional(readOnly = true)
    public CoursePrerequisiteCheckDto checkPrerequisites(Long userId, Long offeringId) {
        CourseOffering offering = resolveVisibleOffering(userId, offeringId, Instant.now());
        return prerequisiteService.check(offering, userId);
    }

    /**
     * Detail visibility: an own active registration, or an offering in the user's audience that is
     * still open for registration. Held course types are not hidden here; that is a list filter.
     */
    private CourseOffering resolveVisibleOffering(Long userId, Long offeringId, Instant now) {
        if (registrationRepository.existsByOffering_IdAndUserIdAndStatusIn(
                offeringId, userId, CourseOfferingListSpecs.ACTIVE_SELF_REGISTRATION_STATUSES)) {
            return offeringRepository
                    .findOne(CourseOfferingListSpecs.withId(offeringId))
                    .orElseThrow(() -> new CourseOfferingNotFoundException(offeringId));
        }
        CourseAudienceScope scope = scopeService.forUser(userId);
        return offeringRepository
                .findOne(
                        Specification.allOf(
                                CourseOfferingListSpecs.withId(offeringId),
                                eligibleOfferings(scope, now)))
                .orElseThrow(() -> new CourseOfferingNotFoundException(offeringId));
    }

    /** Union of the selected registration windows / seat states. */
    private static Specification<CourseOffering> matchingRegistration(
            Set<SelfCourseOfferingRegistrationState> states,
            Instant now,
            Set<Long> fullOfferingIds) {
        return states.stream()
                .map(
                        state ->
                                switch (state) {
                                    case OPEN -> CourseOfferingListSpecs.registrationOpen(now);
                                    case CLOSED ->
                                            Specification.not(
                                                    CourseOfferingListSpecs.registrationOpen(now));
                                    case FULL -> CourseOfferingListSpecs.idIn(fullOfferingIds);
                                })
                .reduce(Specification::or)
                .orElseThrow();
    }

    /** Union of the caller's own registration states. */
    private static Specification<CourseOffering> matchingParticipation(
            Set<SelfCourseOfferingParticipationState> states, Long userId) {
        return states.stream()
                .map(
                        state ->
                                CourseOfferingListSpecs.userHasRegistration(
                                        userId, Set.of(toRegistrationStatus(state))))
                .reduce(Specification::or)
                .orElseThrow();
    }

    private static CourseRegistrationStatus toRegistrationStatus(
            SelfCourseOfferingParticipationState state) {
        return switch (state) {
            case REQUESTED -> CourseRegistrationStatus.PENDING;
            case PARTICIPANT -> CourseRegistrationStatus.PARTICIPANT;
            case WAITLIST -> CourseRegistrationStatus.WAITLIST;
        };
    }

    /** Offerings among {@code offerings} that {@link #isFull} for the caller's scope. */
    private Set<Long> fullOfferingIds(List<CourseOffering> offerings, CourseAudienceScope scope) {
        if (offerings.isEmpty()) {
            return Set.of();
        }
        Set<Long> ids = offerings.stream().map(CourseOffering::getId).collect(Collectors.toSet());
        Map<Long, Long> participantCounts =
                registrationRepository.countParticipants(ids).stream()
                        .collect(
                                Collectors.toMap(
                                        CourseRegistrationRepository.ParticipantCount
                                                ::getOfferingId,
                                        CourseRegistrationRepository.ParticipantCount::getCount));
        Set<Long> quotaAudienceIds =
                offerings.stream()
                        .flatMap(offering -> offering.getAudiences().stream())
                        .filter(entry -> entry.getQuota() != null)
                        .map(CourseOfferingAudience::getId)
                        .collect(Collectors.toSet());
        Map<Long, Long> participantsByAudience =
                quotaAudienceIds.isEmpty()
                        ? Map.of()
                        : registrationRepository
                                .countParticipantsByAudience(quotaAudienceIds)
                                .stream()
                                .collect(
                                        Collectors.toMap(
                                                CourseRegistrationRepository.AudienceSeatCount
                                                        ::getAudienceId,
                                                CourseRegistrationRepository.AudienceSeatCount
                                                        ::getCount));
        return offerings.stream()
                .filter(
                        offering ->
                                isFull(offering, participantCounts, participantsByAudience, scope))
                .map(CourseOffering::getId)
                .collect(Collectors.toSet());
    }

    /** Published offerings still open for registration that match the user's audience. */
    private static Specification<CourseOffering> eligibleOfferings(
            CourseAudienceScope scope, Instant now) {
        return Specification.allOf(
                CourseOfferingListSpecs.published(),
                CourseOfferingListSpecs.registrationNotEnded(now),
                CourseOfferingListSpecs.eligibleFor(scope));
    }

    private SelfCourseOfferingDto toSingleDto(Long userId, CourseOffering offering, Instant now) {
        Set<Long> heldTypeIds =
                new HashSet<>(submissionRepository.findGrantedTypeIdsByUser(userId));
        String typeName =
                trainingCourseTypeRepository
                        .findById(offering.getTrainingCourseTypeId())
                        .map(TenantTrainingCourseType::getName)
                        .orElse(null);
        String organizationName =
                organizationRepository
                        .findById(offering.getOrganizationId())
                        .map(Organization::getName)
                        .orElse(null);
        CourseAudienceScope scope = scopeService.forUser(userId);
        Set<Long> quotaAudienceIds =
                offering.getAudiences().stream()
                        .filter(entry -> entry.getQuota() != null)
                        .map(CourseOfferingAudience::getId)
                        .collect(Collectors.toSet());
        Map<Long, Long> participantsByAudience =
                quotaAudienceIds.isEmpty()
                        ? Map.of()
                        : registrationRepository
                                .countParticipantsByAudience(quotaAudienceIds)
                                .stream()
                                .collect(
                                        Collectors.toMap(
                                                CourseRegistrationRepository.AudienceSeatCount
                                                        ::getAudienceId,
                                                CourseRegistrationRepository.AudienceSeatCount
                                                        ::getCount));
        long participants =
                registrationRepository.countByOffering_IdAndStatus(
                        offering.getId(), CourseRegistrationStatus.PARTICIPANT);
        Map<Long, Long> participantCounts = Map.of(offering.getId(), participants);
        CourseRegistration myRegistration =
                activeRegistrationsByOffering(userId, List.of(offering.getId()))
                        .get(offering.getId());
        Integer waitlistPosition = null;
        if (myRegistration != null
                && myRegistration.getStatus() == CourseRegistrationStatus.WAITLIST) {
            waitlistPosition =
                    waitlistPositions(List.of(myRegistration)).get(myRegistration.getId());
        }
        boolean prerequisitesMet =
                prerequisiteService.unmetOfferingIds(List.of(offering), userId).isEmpty();
        return toDto(
                offering,
                Map.of(offering.getTrainingCourseTypeId(), typeName),
                Map.of(offering.getOrganizationId(), organizationName),
                heldTypeIds,
                participantCounts,
                participantsByAudience,
                scope,
                now,
                myRegistration,
                waitlistPosition,
                prerequisitesMet);
    }

    private Map<Long, CourseRegistration> activeRegistrationsByOffering(
            Long userId, List<Long> offeringIds) {
        if (offeringIds.isEmpty()) {
            return Map.of();
        }
        return registrationRepository
                .findByUserIdAndOffering_IdInAndStatusIn(
                        userId,
                        offeringIds,
                        CourseOfferingListSpecs.ACTIVE_SELF_REGISTRATION_STATUSES)
                .stream()
                .collect(
                        Collectors.toMap(
                                registration -> registration.getOffering().getId(),
                                Function.identity(),
                                (left, right) -> left));
    }

    private Map<Long, Integer> waitlistPositions(Iterable<CourseRegistration> registrations) {
        Map<Long, Integer> positions = new HashMap<>();
        for (CourseRegistration registration : registrations) {
            if (registration.getStatus() != CourseRegistrationStatus.WAITLIST) {
                continue;
            }
            positions.putAll(
                    CourseRegistrationAssembler.waitlistPositions(
                            registrationRepository.findWaitlistInOrder(
                                    registration.getOffering().getId())));
        }
        return positions;
    }

    private SelfCourseOfferingDto toDto(
            CourseOffering offering,
            Map<Long, String> typeNames,
            Map<Long, String> organizationNames,
            Set<Long> heldTypeIds,
            Map<Long, Long> participantCounts,
            Map<Long, Long> participantsByAudience,
            CourseAudienceScope scope,
            Instant now,
            CourseRegistration myRegistration,
            Integer waitlistPosition,
            boolean prerequisitesMet) {
        return SelfCourseOfferingDto.builder()
                .id(offering.getId())
                .organizationId(offering.getOrganizationId())
                .organizationName(organizationNames.get(offering.getOrganizationId()))
                .trainingCourseTypeId(offering.getTrainingCourseTypeId())
                .trainingCourseTypeName(typeNames.get(offering.getTrainingCourseTypeId()))
                .content(offering.getContent())
                .notes(offering.getNotes())
                .registrationStart(offering.getRegistrationStart())
                .registrationEnd(offering.getRegistrationEnd())
                .registrationOpen(offering.isRegistrationOpen(now))
                .full(isFull(offering, participantCounts, participantsByAudience, scope))
                .prerequisitesMet(prerequisitesMet)
                .maxParticipants(offering.getMaxParticipants())
                .hasExam(offering.isHasExam())
                .requiresHomeOrgApproval(offering.isRequiresHomeOrgApproval())
                .alreadyHeld(heldTypeIds.contains(offering.getTrainingCourseTypeId()))
                .firstSessionStart(offering.getFirstSessionStart())
                .lastSessionEnd(offering.getLastSessionEnd())
                .sessions(offering.getSessions().stream().map(this::toSessionDto).toList())
                .myRegistration(toMyRegistrationDto(myRegistration, waitlistPosition, now))
                .build();
    }

    private static SelfCourseOfferingMyRegistrationDto toMyRegistrationDto(
            CourseRegistration registration, Integer waitlistPosition, Instant now) {
        if (registration == null) {
            return null;
        }
        return SelfCourseOfferingMyRegistrationDto.builder()
                .id(registration.getId())
                .status(registration.getStatus())
                .waitlistPosition(waitlistPosition)
                .registeredAt(registration.getRegisteredAt())
                .endedBy(registration.getEndedBy())
                .endReason(registration.getEndReason())
                .workflowId(registration.getWorkflowId())
                .cancellationWorkflowId(registration.getCancellationWorkflowId())
                .canCancel(CourseRegistrationService.canCancel(registration, now))
                .cancellationPending(
                        registration.getStatus() == CourseRegistrationStatus.PARTICIPANT
                                && registration.getCancellationWorkflowId() != null)
                .canWithdraw(registration.getStatus() == CourseRegistrationStatus.WAITLIST)
                .build();
    }

    /**
     * Whether a registration of this user would land on the waitlist: the overall maximum is
     * reached or, with quotas, every audience entry the user matches has used up its quota.
     */
    private static boolean isFull(
            CourseOffering offering,
            Map<Long, Long> participantCounts,
            Map<Long, Long> participantsByAudience,
            CourseAudienceScope scope) {
        if (participantCounts.getOrDefault(offering.getId(), 0L) >= offering.getMaxParticipants()) {
            return true;
        }
        if (offering.getAudiences().stream().noneMatch(entry -> entry.getQuota() != null)) {
            return false;
        }
        return CourseAudienceMatcher.firstWithFreeSeat(
                        CourseAudienceMatcher.matching(offering, scope), participantsByAudience)
                .isEmpty();
    }

    private CourseSessionDto toSessionDto(CourseSession session) {
        return CourseSessionDto.builder()
                .id(session.getId())
                .type(session.getType())
                .start(session.getStart())
                .end(session.getEnd())
                .location(session.getLocation())
                .notes(session.getNotes())
                .build();
    }
}
