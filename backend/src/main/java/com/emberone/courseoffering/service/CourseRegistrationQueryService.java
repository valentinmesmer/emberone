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
import com.emberone.courseoffering.dto.CourseRegistrationDto;
import com.emberone.courseoffering.dto.CourseRegistrationPageDto;
import com.emberone.courseoffering.dto.CourseRegistrationSummaryDto;
import com.emberone.courseoffering.dto.OrganizationCourseRegistrationDto;
import com.emberone.courseoffering.dto.OrganizationCourseRegistrationPageDto;
import com.emberone.courseoffering.dto.SelfCourseRegistrationDto;
import com.emberone.courseoffering.dto.SelfCourseRegistrationPageDto;
import com.emberone.courseoffering.dto.UserCourseRegistrationDto;
import com.emberone.courseoffering.dto.UserCourseRegistrationPageDto;
import com.emberone.courseoffering.exception.CourseOfferingNotFoundException;
import com.emberone.courseoffering.exception.CourseRegistrationNotFoundException;
import com.emberone.courseoffering.list.CourseRegistrationListDefinition;
import com.emberone.courseoffering.list.CourseRegistrationListFilter;
import com.emberone.courseoffering.list.CourseRegistrationListSpecs;
import com.emberone.courseoffering.list.OrganizationCourseRegistrationListDefinition;
import com.emberone.courseoffering.list.UserCourseRegistrationListDefinition;
import com.emberone.courseoffering.list.UserCourseRegistrationListFilter;
import com.emberone.courseoffering.repository.CourseOfferingRepository;
import com.emberone.courseoffering.repository.CourseRegistrationRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read side of registrations: per offering for its organization, per member for any organization.
 */
@Service
@RequiredArgsConstructor
public class CourseRegistrationQueryService {

    private final CourseOfferingRepository offeringRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final CourseRegistrationAssembler assembler;
    private final TenantTrainingCourseTypeRepository trainingCourseTypeRepository;
    private final OrganizationRepository organizationRepository;
    private final TokenService tokenService;

    @Transactional(readOnly = true)
    public CourseRegistrationPageDto listForOffering(
            Long organizationId,
            Long offeringId,
            ListQuery query,
            CourseRegistrationListFilter filter) {
        requireOffering(organizationId, offeringId);
        Page<CourseRegistration> page =
                registrationRepository.findAll(
                        Specification.allOf(
                                CourseRegistrationListSpecs.forOffering(offeringId),
                                CourseRegistrationListSpecs.withStatus(filter.statuses()),
                                CourseRegistrationListDefinition.INSTANCE.searchSpecification(
                                        query)),
                        CourseRegistrationListDefinition.INSTANCE.pageable(query));
        return CourseRegistrationPageDto.builder()
                .content(assembler.toDtos(offeringId, page.getContent()))
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    @Transactional(readOnly = true)
    public CourseRegistrationDto getForOffering(
            Long organizationId, Long offeringId, Long registrationId) {
        requireOffering(organizationId, offeringId);
        CourseRegistration registration =
                registrationRepository
                        .findByIdAndOffering_Id(registrationId, offeringId)
                        .orElseThrow(() -> new CourseRegistrationNotFoundException(registrationId));
        return assembler.toDtos(offeringId, List.of(registration)).get(0);
    }

    @Transactional(readOnly = true)
    public CourseRegistrationSummaryDto summary(Long organizationId, Long offeringId) {
        CourseOffering offering = requireOffering(organizationId, offeringId);
        Map<CourseRegistrationStatus, Long> counts = new EnumMap<>(CourseRegistrationStatus.class);
        registrationRepository
                .countByStatus(offeringId)
                .forEach(row -> counts.put(row.getStatus(), row.getCount()));
        long participants = counts.getOrDefault(CourseRegistrationStatus.PARTICIPANT, 0L);
        return CourseRegistrationSummaryDto.builder()
                .maxParticipants(offering.getMaxParticipants())
                .participants(participants)
                .waitlist(counts.getOrDefault(CourseRegistrationStatus.WAITLIST, 0L))
                .pending(counts.getOrDefault(CourseRegistrationStatus.PENDING, 0L))
                .freeSeats(Math.max(0, offering.getMaxParticipants() - participants))
                .quotas(quotaUsage(offering))
                .build();
    }

    private List<CourseRegistrationSummaryDto.Quota> quotaUsage(CourseOffering offering) {
        List<CourseOfferingAudience> entries =
                offering.getAudiences().stream().filter(e -> e.getQuota() != null).toList();
        if (entries.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> participants = new HashMap<>();
        Map<Long, Long> waitlist = new HashMap<>();
        registrationRepository
                .countSeatsByAudience(offering.getId())
                .forEach(
                        row ->
                                (row.getStatus() == CourseRegistrationStatus.PARTICIPANT
                                                ? participants
                                                : waitlist)
                                        .put(row.getAudienceId(), row.getCount()));
        return entries.stream()
                .sorted(Comparator.comparing(CourseOfferingAudience::getId))
                .map(
                        entry -> {
                            long taken = participants.getOrDefault(entry.getId(), 0L);
                            return CourseRegistrationSummaryDto.Quota.builder()
                                    .audienceId(entry.getId())
                                    .kind(entry.getKind())
                                    .refId(entry.getRefId())
                                    .quota(entry.getQuota())
                                    .participants(taken)
                                    .waitlist(waitlist.getOrDefault(entry.getId(), 0L))
                                    .freeSeats(Math.max(0, entry.getQuota() - taken))
                                    .build();
                        })
                .toList();
    }

    /** Registrations of all offerings of the organization, for the members area. */
    @Transactional(readOnly = true)
    public OrganizationCourseRegistrationPageDto listForOrganization(
            Long organizationId, ListQuery query, UserCourseRegistrationListFilter filter) {
        Specification<CourseRegistration> spec =
                Specification.allOf(
                        CourseRegistrationListSpecs.forOrganization(organizationId),
                        CourseRegistrationListSpecs.withStatus(filter.statuses()),
                        OrganizationCourseRegistrationListDefinition.INSTANCE.searchSpecification(
                                query));
        if (filter.upcomingOnly()) {
            spec = spec.and(CourseRegistrationListSpecs.notEndedAt(Instant.now()));
        }
        Page<CourseRegistration> page =
                registrationRepository.findAll(
                        spec,
                        OrganizationCourseRegistrationListDefinition.INSTANCE.pageable(query));
        OfferingLookup lookup = lookup(page.getContent());
        return OrganizationCourseRegistrationPageDto.builder()
                .content(page.getContent().stream().map(r -> toOrganizationDto(r, lookup)).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    /**
     * Registrations of one member for the member view. Callers must already have checked that the
     * member belongs to the organization and that the caller may read training courses there.
     */
    @Transactional(readOnly = true)
    public UserCourseRegistrationPageDto listForUser(
            Long userId, ListQuery query, UserCourseRegistrationListFilter filter) {
        Specification<CourseRegistration> spec =
                Specification.allOf(
                        CourseRegistrationListSpecs.forUser(userId),
                        CourseRegistrationListSpecs.withStatus(filter.statuses()),
                        UserCourseRegistrationListDefinition.INSTANCE.searchSpecification(query));
        if (filter.upcomingOnly()) {
            spec = spec.and(CourseRegistrationListSpecs.notEndedAt(Instant.now()));
        }
        Page<CourseRegistration> page =
                registrationRepository.findAll(
                        spec, UserCourseRegistrationListDefinition.INSTANCE.pageable(query));
        OfferingLookup lookup = lookup(page.getContent());
        return UserCourseRegistrationPageDto.builder()
                .content(
                        page.getContent().stream()
                                .map(
                                        r ->
                                                toUserDto(
                                                        r,
                                                        lookup.offerings().get(offeringId(r)),
                                                        lookup))
                                .toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    /** The caller's own registrations, with what the caller may still do with each. */
    @Transactional(readOnly = true)
    public SelfCourseRegistrationPageDto listForSelf(Long userId, ListQuery query) {
        Instant now = Instant.now();
        Page<CourseRegistration> page = findForUser(userId, query);
        OfferingLookup lookup = lookup(page.getContent());
        Map<Long, Integer> positions = waitlistPositions(page.getContent());
        return SelfCourseRegistrationPageDto.builder()
                .content(
                        page.getContent().stream()
                                .map(
                                        r ->
                                                toSelfDto(
                                                        r,
                                                        lookup.offerings().get(offeringId(r)),
                                                        lookup,
                                                        positions.get(r.getId()),
                                                        now))
                                .toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    @Transactional(readOnly = true)
    public SelfCourseRegistrationDto getForSelf(Long userId, Long registrationId) {
        Instant now = Instant.now();
        CourseRegistration registration =
                registrationRepository
                        .findByIdAndUserId(registrationId, userId)
                        .orElseThrow(() -> new CourseRegistrationNotFoundException(registrationId));
        OfferingLookup lookup = lookup(List.of(registration));
        CourseOffering offering = lookup.offerings().get(offeringId(registration));
        if (offering == null) {
            throw new CourseOfferingNotFoundException(offeringId(registration));
        }
        Integer waitlistPosition = null;
        if (registration.getStatus() == CourseRegistrationStatus.WAITLIST) {
            waitlistPosition =
                    CourseRegistrationAssembler.waitlistPositions(
                                    registrationRepository.findWaitlistInOrder(offering.getId()))
                            .get(registrationId);
        }
        return toSelfDto(registration, offering, lookup, waitlistPosition, now);
    }

    private Page<CourseRegistration> findForUser(Long userId, ListQuery query) {
        return registrationRepository.findAll(
                CourseRegistrationListSpecs.forUser(userId),
                UserCourseRegistrationListDefinition.INSTANCE.pageable(query));
    }

    /** Offerings, course type names and organization names for a page of registrations. */
    private record OfferingLookup(
            Map<Long, CourseOffering> offerings,
            Map<Long, String> typeNames,
            Map<Long, String> organizationNames) {}

    private OfferingLookup lookup(List<CourseRegistration> registrations) {
        List<Long> offeringIds = registrations.stream().map(r -> offeringId(r)).distinct().toList();
        Map<Long, CourseOffering> offerings =
                offeringIds.isEmpty()
                        ? Map.of()
                        : offeringRepository.findByIdIn(offeringIds).stream()
                                .collect(
                                        Collectors.toMap(
                                                CourseOffering::getId, Function.identity()));
        Map<Long, String> typeNames =
                trainingCourseTypeRepository
                        .findAllById(
                                offerings.values().stream()
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
                                offerings.values().stream()
                                        .map(CourseOffering::getOrganizationId)
                                        .distinct()
                                        .toList())
                        .stream()
                        .collect(Collectors.toMap(Organization::getId, Organization::getName));
        return new OfferingLookup(offerings, typeNames, organizationNames);
    }

    /** 1-based waitlist places of the waitlisted registrations on the page, per registration id. */
    private Map<Long, Integer> waitlistPositions(List<CourseRegistration> registrations) {
        Map<Long, Integer> positions = new HashMap<>();
        registrations.stream()
                .filter(r -> r.getStatus() == CourseRegistrationStatus.WAITLIST)
                .map(r -> offeringId(r))
                .distinct()
                .forEach(
                        offeringId -> {
                            positions.putAll(
                                    CourseRegistrationAssembler.waitlistPositions(
                                            registrationRepository.findWaitlistInOrder(
                                                    offeringId)));
                        });
        return positions;
    }

    private static Long offeringId(CourseRegistration registration) {
        return registration.getOffering().getId();
    }

    private static OrganizationCourseRegistrationDto toOrganizationDto(
            CourseRegistration registration, OfferingLookup lookup) {
        CourseOffering offering = lookup.offerings().get(offeringId(registration));
        return OrganizationCourseRegistrationDto.builder()
                .id(registration.getId())
                .offeringId(offering.getId())
                .userId(registration.getUserId())
                .userDisplayName(CourseRegistrationAssembler.displayName(registration.getUser()))
                .trainingCourseTypeName(lookup.typeNames().get(offering.getTrainingCourseTypeId()))
                .firstSessionStart(offering.getFirstSessionStart())
                .lastSessionEnd(offering.getLastSessionEnd())
                .status(registration.getStatus())
                .build();
    }

    private static UserCourseRegistrationDto toUserDto(
            CourseRegistration registration, CourseOffering offering, OfferingLookup lookup) {
        return UserCourseRegistrationDto.builder()
                .id(registration.getId())
                .offeringId(offering.getId())
                .trainingCourseTypeId(offering.getTrainingCourseTypeId())
                .trainingCourseTypeName(lookup.typeNames().get(offering.getTrainingCourseTypeId()))
                .organizationId(offering.getOrganizationId())
                .organizationName(lookup.organizationNames().get(offering.getOrganizationId()))
                .firstSessionStart(offering.getFirstSessionStart())
                .lastSessionEnd(offering.getLastSessionEnd())
                .status(registration.getStatus())
                .endedBy(registration.getEndedBy())
                .build();
    }

    private static SelfCourseRegistrationDto toSelfDto(
            CourseRegistration registration,
            CourseOffering offering,
            OfferingLookup lookup,
            Integer waitlistPosition,
            Instant now) {
        return SelfCourseRegistrationDto.builder()
                .id(registration.getId())
                .offeringId(offering.getId())
                .trainingCourseTypeId(offering.getTrainingCourseTypeId())
                .trainingCourseTypeName(lookup.typeNames().get(offering.getTrainingCourseTypeId()))
                .organizationId(offering.getOrganizationId())
                .organizationName(lookup.organizationNames().get(offering.getOrganizationId()))
                .firstSessionStart(offering.getFirstSessionStart())
                .lastSessionEnd(offering.getLastSessionEnd())
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

    private CourseOffering requireOffering(Long organizationId, Long offeringId) {
        return offeringRepository
                .findByIdAndOrganizationId(offeringId, organizationId)
                .orElseThrow(() -> new CourseOfferingNotFoundException(offeringId));
    }
}
