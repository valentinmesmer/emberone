/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.service;

import com.emberone.event.domain.EventType;
import com.emberone.event.domain.OperationEvent;
import com.emberone.event.domain.ServiceEvent;
import com.emberone.event.dto.*;
import com.emberone.event.dto.OperationEventPageDto;
import com.emberone.event.dto.ServiceEventPageDto;
import com.emberone.event.event.EventChangedEvent;
import com.emberone.event.event.EventDeletingEvent;
import com.emberone.event.event.EventPromotedFromAppointmentEvent;
import com.emberone.event.exception.EventNotFoundException;
import com.emberone.event.exception.EventValidationException;
import com.emberone.event.list.EventListFilter;
import com.emberone.event.list.EventListSpecs;
import com.emberone.event.list.OperationEventListDefinition;
import com.emberone.event.list.ServiceEventListDefinition;
import com.emberone.event.mapper.EventDtoAssembler;
import com.emberone.event.mapper.EventMapper;
import com.emberone.event.repository.OperationEventRepository;
import com.emberone.event.repository.ServiceEventRepository;
import com.emberone.event.support.EventListDateRange;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.time.OrganizationWallClockTime;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.PageReorder;
import com.emberone.user.domain.User;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final ServiceEventRepository serviceEventRepository;
    private final OperationEventRepository operationEventRepository;
    private final EventMapper eventMapper;
    private final EventDtoAssembler eventDtoAssembler;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;

    private static <T> Specification<T> eventTimeFilter(EventListFilter filter) {
        if (!filter.hasDateRange()) {
            return (root, query, cb) -> cb.conjunction();
        }
        EventListDateRange.Resolved range = EventListDateRange.resolve(filter.from(), filter.to());
        return EventListSpecs.startAtRange(range.from(), range.to());
    }

    private static void validateEventTimeRange(Instant start, Instant end) {
        if (start == null || end == null) {
            throw new EventValidationException("Start and end are required");
        }
        if (end.isBefore(start)) {
            throw new EventValidationException("End must not be before start");
        }
    }

    private Long requireTenantId() {
        Long tenantId = tokenService.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No tenantId found in token");
        }
        return tenantId;
    }

    private void validateOrganizationIdConsistency(
            Long expectedOrganizationId, Long actualOrganizationId) {
        if (!expectedOrganizationId.equals(actualOrganizationId)) {
            log.warn(
                    "Organization ID mismatch: expected {}, but got {}",
                    expectedOrganizationId,
                    actualOrganizationId);
            throw new EventValidationException(
                    "Organization ID in request does not match the path parameter");
        }
    }

    private Organization requireOrganizationInTenant(Long organizationId, Long tenantId) {
        Organization organization =
                organizationRepository
                        .findById(organizationId)
                        .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        if (!tenantId.equals(organization.getTenantId())) {
            throw new OrganizationNotFoundException(organizationId);
        }
        return organization;
    }

    private User requireLeaderInTenant(Long leaderId, Long tenantId) {
        User leader =
                userRepository
                        .findById(leaderId)
                        .orElseThrow(() -> new UserNotFoundException(leaderId));
        if (!tenantId.equals(leader.getTenantId())) {
            throw new EventValidationException("Leader does not belong to the tenant");
        }
        return leader;
    }

    public void assertEventInOrganization(EventType eventType, Long eventId, Long organizationId) {
        boolean ok =
                switch (eventType) {
                    case SERVICE ->
                            serviceEventRepository
                                    .findByIdAndOrganizationId(eventId, organizationId)
                                    .isPresent();
                    case OPERATION ->
                            operationEventRepository
                                    .findByIdAndOrganizationId(eventId, organizationId)
                                    .isPresent();
                };
        if (!ok) {
            throw new EventNotFoundException(
                    "Event not found or does not belong to the organization");
        }
    }

    /** Calendar day of the event start (used e.g. for certification grants). */
    @Transactional(readOnly = true)
    public LocalDate requireEventStartDate(EventType eventType, Long eventId, Long organizationId) {
        Instant start =
                switch (eventType) {
                    case SERVICE ->
                            serviceEventRepository
                                    .findByIdAndOrganizationId(eventId, organizationId)
                                    .map(ServiceEvent::getStart)
                                    .orElse(null);
                    case OPERATION ->
                            operationEventRepository
                                    .findByIdAndOrganizationId(eventId, organizationId)
                                    .map(OperationEvent::getStart)
                                    .orElse(null);
                };
        if (start == null) {
            throw new EventNotFoundException(
                    "Event not found or does not belong to the organization");
        }
        return OrganizationWallClockTime.toOrganizationDate(start);
    }

    @Transactional(readOnly = true)
    public List<Integer> listDistinctYearsForOrganization(
            Long organizationId, EventType eventType) {
        ZoneId zone = OrganizationWallClockTime.organizationZone();
        SortedSet<Integer> years = new TreeSet<>(Comparator.reverseOrder());
        List<Instant> starts =
                switch (eventType) {
                    case SERVICE ->
                            serviceEventRepository.findStartsByOrganizationId(organizationId);
                    case OPERATION ->
                            operationEventRepository.findStartsByOrganizationId(organizationId);
                };
        for (Instant start : starts) {
            years.add(start.atZone(zone).getYear());
        }
        years.add(OrganizationWallClockTime.today().getYear());
        return List.copyOf(years);
    }

    @Transactional(readOnly = true)
    public ServiceEventDto getServiceEvent(Long organizationId, Long eventId) {
        return eventMapper.toDto(requireServiceEventInOrganization(organizationId, eventId));
    }

    @Transactional(readOnly = true)
    public OperationEventDto getOperationEvent(Long organizationId, Long eventId) {
        return eventMapper.toDto(requireOperationEventInOrganization(organizationId, eventId));
    }

    private ServiceEvent requireServiceEventInOrganization(Long organizationId, Long eventId) {
        Long tenantId = requireTenantId();
        List<ServiceEvent> hydrated = serviceEventRepository.findWithLeaderByIdIn(List.of(eventId));
        return hydrated.stream()
                .filter(
                        event ->
                                tenantId.equals(event.getTenantId())
                                        && Objects.equals(
                                                organizationId, event.getOrganization().getId()))
                .findFirst()
                .orElseThrow(
                        () ->
                                new EventNotFoundException(
                                        "Event not found or does not belong to the organization"));
    }

    private OperationEvent requireOperationEventInOrganization(Long organizationId, Long eventId) {
        Long tenantId = requireTenantId();
        List<OperationEvent> hydrated =
                operationEventRepository.findWithLeaderByIdIn(List.of(eventId));
        return hydrated.stream()
                .filter(
                        event ->
                                tenantId.equals(event.getTenantId())
                                        && Objects.equals(
                                                organizationId, event.getOrganization().getId()))
                .findFirst()
                .orElseThrow(
                        () ->
                                new EventNotFoundException(
                                        "Event not found or does not belong to the organization"));
    }

    @Transactional(readOnly = true)
    public ServiceEventPageDto listServiceEvents(
            Long organizationId, ListQuery query, EventListFilter filter) {
        return listServiceEvents(organizationId, query, filter, tokenService.requireUserId());
    }

    private ServiceEventPageDto listServiceEvents(
            Long organizationId, ListQuery query, EventListFilter filter, Long userId) {
        Specification<ServiceEvent> timeFilter = eventTimeFilter(filter);
        Pageable pageable = ServiceEventListDefinition.INSTANCE.pageable(query);
        Specification<ServiceEvent> spec =
                Specification.allOf(
                        EventListSpecs.forOrganization(organizationId),
                        timeFilter,
                        EventListSpecs.overlapsCalendarYears(filter.years()),
                        EventListSpecs.attendanceFilter(
                                EventType.SERVICE, userId, filter.attendance()),
                        ServiceEventListDefinition.INSTANCE.searchSpecification(query));
        Page<ServiceEvent> entityPage = serviceEventRepository.findAll(spec, pageable);
        List<Long> ids = entityPage.getContent().stream().map(ServiceEvent::getId).toList();
        List<ServiceEvent> hydrated =
                ids.isEmpty() ? List.of() : serviceEventRepository.findWithLeaderByIdIn(ids);
        List<ServiceEvent> ordered = PageReorder.reorderByIds(ids, hydrated, ServiceEvent::getId);
        Page<ServiceEvent> result = PageReorder.pageOf(ordered, entityPage);
        return eventDtoAssembler.toServicePageDto(result);
    }

    @Transactional(readOnly = true)
    public OperationEventPageDto listOperationEvents(
            Long organizationId, ListQuery query, EventListFilter filter) {
        return listOperationEvents(organizationId, query, filter, tokenService.requireUserId());
    }

    private OperationEventPageDto listOperationEvents(
            Long organizationId, ListQuery query, EventListFilter filter, Long userId) {
        Specification<OperationEvent> timeFilter = eventTimeFilter(filter);
        Pageable pageable = OperationEventListDefinition.INSTANCE.pageable(query);
        Specification<OperationEvent> spec =
                Specification.allOf(
                        EventListSpecs.forOrganization(organizationId),
                        timeFilter,
                        EventListSpecs.overlapsCalendarYears(filter.years()),
                        EventListSpecs.attendanceFilter(
                                EventType.OPERATION, userId, filter.attendance()),
                        OperationEventListDefinition.INSTANCE.searchSpecification(query));
        Page<OperationEvent> entityPage = operationEventRepository.findAll(spec, pageable);
        List<Long> ids = entityPage.getContent().stream().map(OperationEvent::getId).toList();
        List<OperationEvent> hydrated =
                ids.isEmpty() ? List.of() : operationEventRepository.findWithLeaderByIdIn(ids);
        List<OperationEvent> ordered =
                PageReorder.reorderByIds(ids, hydrated, OperationEvent::getId);
        Page<OperationEvent> result = PageReorder.pageOf(ordered, entityPage);
        return eventDtoAssembler.toOperationPageDto(result);
    }

    @Transactional
    public ServiceEventDto createServiceEvent(
            Long userId, Long organizationId, CreateServiceEventRequest request) {
        validateOrganizationIdConsistency(organizationId, request.getOrganizationId());
        validateEventTimeRange(request.getStart(), request.getEnd());

        Long tenantId = requireTenantId();
        Organization organization =
                requireOrganizationInTenant(request.getOrganizationId(), tenantId);

        ServiceEvent event =
                ServiceEvent.builder()
                        .tenantId(tenantId)
                        .start(request.getStart())
                        .end(request.getEnd())
                        .organization(organization)
                        .location(request.getLocation())
                        .description(request.getDescription())
                        .topic(request.getTopic())
                        .build();

        if (request.getLeaderId() != null) {
            event.setLeader(requireLeaderInTenant(request.getLeaderId(), tenantId));
        }

        ServiceEvent saved = serviceEventRepository.save(event);
        if (request.getPromoteFromAppointmentId() != null) {
            eventPublisher.publishEvent(
                    new EventPromotedFromAppointmentEvent(
                            organizationId,
                            request.getPromoteFromAppointmentId(),
                            EventType.SERVICE,
                            request.getStart(),
                            request.getEnd()));
        }
        eventPublisher.publishEvent(
                new EventChangedEvent(tenantId, organizationId, EventType.SERVICE, saved.getId()));
        if (request.getPromoteFromAppointmentId() != null) {
            log.info(
                    "Promoted appointment {} to service event {} in organization {}",
                    request.getPromoteFromAppointmentId(),
                    saved.getId(),
                    organizationId);
        } else {
            log.info("Created service event {} in organization {}", saved.getId(), organizationId);
        }
        return eventMapper.toDto(saved);
    }

    @Transactional
    public OperationEventDto createOperationEvent(
            Long userId, Long organizationId, CreateOperationEventRequest request) {
        validateOrganizationIdConsistency(organizationId, request.getOrganizationId());
        validateEventTimeRange(request.getStart(), request.getEnd());

        Long tenantId = requireTenantId();
        Organization organization =
                requireOrganizationInTenant(request.getOrganizationId(), tenantId);

        OperationEvent event =
                OperationEvent.builder()
                        .tenantId(tenantId)
                        .start(request.getStart())
                        .end(request.getEnd())
                        .organization(organization)
                        .location(request.getLocation())
                        .description(request.getDescription())
                        .number(request.getNumber())
                        .keyword(request.getKeyword())
                        .tag(request.getTag())
                        .specialSignal(
                                request.getSpecialSignal() != null
                                        ? request.getSpecialSignal()
                                        : false)
                        .build();

        if (request.getLeaderId() != null) {
            event.setLeader(requireLeaderInTenant(request.getLeaderId(), tenantId));
        }

        OperationEvent saved = operationEventRepository.save(event);
        eventPublisher.publishEvent(
                new EventChangedEvent(
                        tenantId, organizationId, EventType.OPERATION, saved.getId()));
        log.info("Created operation event {} in organization {}", saved.getId(), organizationId);
        return eventMapper.toDto(saved);
    }

    @Transactional
    public ServiceEventDto updateServiceEvent(
            Long userId, Long organizationId, Long eventId, UpdateServiceEventRequest request) {
        Long tenantId = requireTenantId();
        ServiceEvent event =
                serviceEventRepository
                        .findByIdAndOrganizationId(eventId, organizationId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Service event not found or does not belong to the organization"));

        eventMapper.updateFromRequest(request, event);
        validateEventTimeRange(event.getStart(), event.getEnd());

        if (request.getLeaderId() != null) {
            event.setLeader(requireLeaderInTenant(request.getLeaderId(), tenantId));
        } else {
            event.setLeader(null);
        }

        ServiceEvent saved = serviceEventRepository.save(event);
        eventPublisher.publishEvent(
                new EventChangedEvent(tenantId, organizationId, EventType.SERVICE, saved.getId()));
        log.info("Updated service event {} in organization {}", saved.getId(), organizationId);
        return eventMapper.toDto(saved);
    }

    @Transactional
    public void deleteServiceEvent(Long userId, Long organizationId, Long eventId) {
        Long tenantId = requireTenantId();
        ServiceEvent event =
                serviceEventRepository
                        .findByIdAndOrganizationId(eventId, organizationId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Service event not found or does not belong to the organization"));

        eventPublisher.publishEvent(new EventDeletingEvent(tenantId, EventType.SERVICE, eventId));
        serviceEventRepository.delete(event);
        log.info("Deleted service event {} in organization {}", eventId, organizationId);
    }

    @Transactional
    public OperationEventDto updateOperationEvent(
            Long userId, Long organizationId, Long eventId, UpdateOperationEventRequest request) {
        Long tenantId = requireTenantId();
        OperationEvent event =
                operationEventRepository
                        .findByIdAndOrganizationId(eventId, organizationId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Operation event not found or does not belong to the organization"));

        eventMapper.updateFromRequest(request, event);
        validateEventTimeRange(event.getStart(), event.getEnd());

        if (request.getLeaderId() != null) {
            event.setLeader(requireLeaderInTenant(request.getLeaderId(), tenantId));
        } else {
            event.setLeader(null);
        }

        OperationEvent saved = operationEventRepository.save(event);
        eventPublisher.publishEvent(
                new EventChangedEvent(
                        tenantId, organizationId, EventType.OPERATION, saved.getId()));
        log.info("Updated operation event {} in organization {}", saved.getId(), organizationId);
        return eventMapper.toDto(saved);
    }

    @Transactional
    public void deleteOperationEvent(Long userId, Long organizationId, Long eventId) {
        Long tenantId = requireTenantId();
        OperationEvent event =
                operationEventRepository
                        .findByIdAndOrganizationId(eventId, organizationId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Operation event not found or does not belong to the organization"));

        eventPublisher.publishEvent(new EventDeletingEvent(tenantId, EventType.OPERATION, eventId));
        operationEventRepository.delete(event);
        log.info("Deleted operation event {} in organization {}", eventId, organizationId);
    }
}
