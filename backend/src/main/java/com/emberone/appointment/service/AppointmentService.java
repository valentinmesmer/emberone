/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import com.emberone.appointment.domain.*;
import com.emberone.appointment.dto.*;
import com.emberone.appointment.event.AppointmentChangeType;
import com.emberone.appointment.event.AppointmentChangedEvent;
import com.emberone.appointment.event.AppointmentDeletingEvent;
import com.emberone.appointment.exception.AppointmentAccessDeniedException;
import com.emberone.appointment.exception.AppointmentNotFoundException;
import com.emberone.appointment.exception.AppointmentValidationException;
import com.emberone.appointment.list.AppointmentListDateRange;
import com.emberone.appointment.list.AppointmentListDefinition;
import com.emberone.appointment.list.AppointmentListFilter;
import com.emberone.appointment.list.AppointmentListSpecs;
import com.emberone.appointment.mapper.AppointmentDtoAssembler;
import com.emberone.appointment.mapper.AppointmentMapper;
import com.emberone.appointment.repository.AppointmentReadRepository;
import com.emberone.appointment.repository.AppointmentRepository;
import com.emberone.appointment.repository.AppointmentSeriesRepository;
import com.emberone.event.domain.EventType;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.security.OrganizationAccessService;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.time.OrganizationWallClockTime;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.PageReorder;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
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
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentSeriesRepository appointmentSeriesRepository;
    private final AppointmentReadRepository appointmentReadRepository;
    private final OrganizationRepository organizationRepository;
    private final UserOrganizationMembershipRepository userOrganizationMembershipRepository;
    private final UserOrganizationGroupMembershipRepository groupMembershipRepository;
    private final AppointmentAudienceService audienceService;
    private final OrganizationAccessService organizationAccessService;
    private final AppointmentRecurrenceService recurrenceService;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentDtoAssembler appointmentDtoAssembler;
    private final AppointmentRequestValidator requestValidator;
    private final TokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;

    private static boolean appointmentTypeMatchesEventType(AppointmentType t, EventType e) {
        return t == AppointmentType.SERVICE && e == EventType.SERVICE;
    }

    private static Instant plusMonths(Instant instant, int months) {
        return instant.atZone(ZoneOffset.UTC).plusMonths(months).toInstant();
    }

    private static Instant minusMonths(Instant instant, int months) {
        return instant.atZone(ZoneOffset.UTC).minusMonths(months).toInstant();
    }

    @Transactional(readOnly = true)
    public AppointmentPageDto listAppointments(
            Long organizationId, ListQuery query, AppointmentListFilter filter) {
        Long userId = tokenService.requireUserId();
        boolean member = organizationAccessService.isActiveOrganizationMember(organizationId);
        if (!member && (!filter.viewed().isEmpty() || !filter.response().isEmpty())) {
            throw new AppointmentValidationException(
                    "Read state and own responses need an active membership");
        }

        Instant now = Instant.now();
        Specification<Appointment> timeFilter;
        if (filter.hasDateRange()) {
            AppointmentListDateRange.Resolved range =
                    AppointmentListDateRange.resolve(filter.from(), filter.to());
            timeFilter =
                    AppointmentListSpecs.overlapsRange(range.fromInclusive(), range.toExclusive());
        } else {
            timeFilter = AppointmentListSpecs.periodsFilter(filter.periods(), now);
        }
        List<Long> activeGroupIds =
                groupMembershipRepository.findActiveGroupIdsForUserInOrganization(
                        userId, organizationId, now);

        Pageable pageable =
                AppointmentListDefinition.INSTANCE.pageable(query, filter.sortOverride());
        Specification<Appointment> spec =
                Specification.allOf(
                        AppointmentListSpecs.forOrganization(organizationId),
                        AppointmentListSpecs.activeOnly(),
                        timeFilter,
                        AppointmentListSpecs.overlapsCalendarYears(filter.years()),
                        AppointmentListSpecs.viewedFilter(userId, filter.viewed()),
                        AppointmentListSpecs.responseFilter(userId, filter.response()),
                        AppointmentListSpecs.visibleToUser(activeGroupIds),
                        AppointmentListDefinition.INSTANCE.searchSpecification(query));

        Page<Appointment> entityPage = appointmentRepository.findAll(spec, pageable);
        List<Long> ids = entityPage.getContent().stream().map(Appointment::getId).toList();
        List<Appointment> hydrated =
                ids.isEmpty()
                        ? List.of()
                        : appointmentRepository.findWithOrganizationAndAudienceByIdIn(ids);
        List<Appointment> ordered = PageReorder.reorderByIds(ids, hydrated, Appointment::getId);
        Page<Appointment> page = PageReorder.pageOf(ordered, entityPage);

        List<AppointmentDto> dtos = appointmentDtoAssembler.toDtos(page.getContent());
        if (member) {
            enrichUnread(userId, dtos);
        }
        return appointmentDtoAssembler.toPageDto(page, dtos);
    }

    @Transactional(readOnly = true)
    public List<Integer> listDistinctYearsForOrganization(Long organizationId) {
        java.time.ZoneId zone = OrganizationWallClockTime.organizationZone();
        SortedSet<Integer> years = new TreeSet<>(Comparator.reverseOrder());
        for (Instant start : appointmentRepository.findStartsByOrganizationId(organizationId)) {
            years.add(start.atZone(zone).getYear());
        }
        years.add(OrganizationWallClockTime.today().getYear());
        return List.copyOf(years);
    }

    @Transactional(readOnly = true)
    public UpcomingAppointmentsDto listUpcomingAcrossOrganizations(int limit) {
        Long userId = tokenService.requireUserId();

        List<UserOrganizationMembership> userOrganizationMemberships =
                userOrganizationMembershipRepository.findActiveByUser_Id(userId, Instant.now());
        Set<Long> orgIds =
                userOrganizationMemberships.stream()
                        .map(UserOrganizationMembership::getOrganization)
                        .map(Organization::getId)
                        .collect(Collectors.toSet());

        Instant now = Instant.now();
        List<Appointment> all = new ArrayList<>();

        for (Long orgId : orgIds) {
            all.addAll(appointmentRepository.findUpcomingByOrganizationId(orgId, now));
        }

        List<Appointment> visibleRaw =
                audienceService.filterAppointmentsVisibleToUser(
                        userId, all.stream().filter(a -> !a.isArchived()).toList());

        long unreadCount = countUnread(userId, visibleRaw);

        List<AppointmentDto> visible =
                enrichUnread(
                        userId,
                        visibleRaw.stream()
                                .map(appointmentMapper::toDto)
                                .sorted(
                                        Comparator.comparing(AppointmentDto::getStart)
                                                .thenComparing(AppointmentDto::getId))
                                .toList());

        return UpcomingAppointmentsDto.builder()
                .content(visible.stream().limit(limit).toList())
                .totalCount(visible.size())
                .unreadCount(unreadCount)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> countUnreadAcrossOrganizations() {
        Long userId = tokenService.requireUserId();

        List<UserOrganizationMembership> memberships =
                userOrganizationMembershipRepository.findActiveByUser_Id(userId, Instant.now());
        Instant now = Instant.now();
        Instant listHorizonEnd =
                plusMonths(now, AppointmentRecurrenceService.SERIES_HORIZON_MONTHS);

        Map<Long, Long> counts = new LinkedHashMap<>();
        for (UserOrganizationMembership membership : memberships) {
            Long orgId = membership.getOrganization().getId();
            List<Appointment> upcoming =
                    appointmentRepository.findUpcomingByOrganizationId(orgId, now);
            List<Appointment> visible =
                    audienceService.filterAppointmentsVisibleToUser(userId, upcoming);
            counts.put(orgId, countUnread(userId, visible, listHorizonEnd));
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public AppointmentDto getAppointment(Long organizationId, Long appointmentId) {
        Long userId = tokenService.requireUserId();
        audienceService.ensureUserCanAccess(userId, appointmentId);

        Appointment appointment = findAppointment(organizationId, appointmentId);
        AppointmentDto dto = appointmentMapper.toDto(appointment);
        if (organizationAccessService.isActiveOrganizationMember(organizationId)) {
            dto.setUnread(
                    !appointmentReadRepository.existsByAppointmentIdAndUserId(
                            appointmentId, userId));
        }
        return dto;
    }

    @Transactional
    public void markRead(Long organizationId, Long appointmentId) {
        Long tenantId = tokenService.requireTenantId();
        Long userId = tokenService.requireUserId();
        audienceService.ensureUserCanAccess(userId, appointmentId);

        Appointment appointment = findAppointment(organizationId, appointmentId);
        saveReadIfAbsent(tenantId, userId, appointment);
    }

    @Transactional
    public void markAllReadForOrganization(Long organizationId) {
        Long tenantId = tokenService.requireTenantId();
        Long userId = tokenService.requireUserId();

        Instant now = Instant.now();
        Instant rangeFrom = minusMonths(now, 1);
        Instant rangeTo = plusMonths(now, AppointmentRecurrenceService.SERIES_HORIZON_MONTHS);
        List<Appointment> appointments =
                appointmentRepository.findByOrganizationIdAndDateRange(
                        organizationId, rangeFrom, rangeTo);
        List<Appointment> visible =
                audienceService.filterAppointmentsVisibleToUser(
                        userId, appointments.stream().filter(a -> !a.isArchived()).toList());
        if (visible.isEmpty()) {
            return;
        }

        List<Long> appointmentIds = visible.stream().map(Appointment::getId).toList();
        Set<Long> alreadyRead =
                new HashSet<>(
                        appointmentReadRepository.findAppointmentIdsByUserIdAndAppointmentIdIn(
                                userId, appointmentIds));
        Instant readAt = Instant.now();
        List<AppointmentRead> toSave =
                visible.stream()
                        .filter(a -> !alreadyRead.contains(a.getId()))
                        .map(
                                a ->
                                        AppointmentRead.builder()
                                                .tenantId(tenantId)
                                                .appointment(a)
                                                .userId(userId)
                                                .readAt(readAt)
                                                .build())
                        .toList();
        if (!toSave.isEmpty()) {
            appointmentReadRepository.saveAll(toSave);
        }
    }

    private void saveReadIfAbsent(Long tenantId, Long userId, Appointment appointment) {
        if (appointmentReadRepository.existsByAppointmentIdAndUserId(appointment.getId(), userId)) {
            return;
        }
        appointmentReadRepository.save(
                AppointmentRead.builder()
                        .tenantId(tenantId)
                        .appointment(appointment)
                        .userId(userId)
                        .readAt(Instant.now())
                        .build());
    }

    @Transactional
    public AppointmentDto createAppointment(Long organizationId, CreateAppointmentRequest request) {

        validateOrganizationIdConsistency(organizationId, request.getOrganizationId());
        requestValidator.normalizeAndValidateCreate(request);

        Organization organization =
                organizationRepository
                        .findById(organizationId)
                        .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        if (request.getRecurrence() != null) {
            return createSeriesAppointment(organization, request);
        }

        Appointment saved = appointmentRepository.save(buildAppointment(organization, request));
        publishChanged(saved, AppointmentChangeType.CREATED);
        log.info("Created appointment {} in organization {}", saved.getId(), organizationId);
        return appointmentMapper.toDto(saved);
    }

    @Transactional
    public AppointmentDto updateAppointment(
            Long organizationId,
            Long appointmentId,
            UpdateAppointmentRequest request,
            SeriesEditScope scope) {
        Long userId = tokenService.requireUserId();
        audienceService.ensureUserCanAccess(userId, appointmentId);

        Appointment appointment = findAppointment(organizationId, appointmentId);
        if (appointment.isArchived()) {
            throw new AppointmentValidationException("Cannot modify an archived appointment");
        }
        requestValidator.normalizeAndValidateUpdate(organizationId, appointment, request);

        SeriesEditScope effectiveScope = scope != null ? scope : SeriesEditScope.THIS_OCCURRENCE;

        if (appointment.getSeries() == null || effectiveScope == SeriesEditScope.THIS_OCCURRENCE) {
            applyUpdate(appointment, request);
            appointment.setIsException(appointment.getSeries() != null);
            Appointment saved = appointmentRepository.save(appointment);
            publishChanged(saved, AppointmentChangeType.UPDATED);
            log.info("Updated appointment {} in organization {}", saved.getId(), organizationId);
            return appointmentMapper.toDto(saved);
        }

        if (effectiveScope == SeriesEditScope.ENTIRE_SERIES) {
            updateEntireSeries(appointment, request);
            log.info(
                    "Updated appointment series {} in organization {}",
                    appointment.getSeries().getId(),
                    organizationId);
            return appointmentMapper.toDto(appointment);
        }

        if (effectiveScope == SeriesEditScope.THIS_AND_FUTURE) {
            splitSeriesAndUpdateFuture(appointment, request);
            log.info(
                    "Updated appointment series {} in organization {} (this and future)",
                    appointment.getSeries().getId(),
                    organizationId);
            return appointmentMapper.toDto(appointment);
        }

        applyUpdate(appointment, request);
        Appointment saved = appointmentRepository.save(appointment);
        publishChanged(saved, AppointmentChangeType.UPDATED);
        log.info("Updated appointment {} in organization {}", saved.getId(), organizationId);
        return appointmentMapper.toDto(saved);
    }

    @Transactional
    public void deleteAppointment(Long organizationId, Long appointmentId, SeriesEditScope scope) {
        Long userId = tokenService.requireUserId();
        audienceService.ensureUserCanAccess(userId, appointmentId);
        Appointment appointment = findAppointment(organizationId, appointmentId);

        SeriesEditScope effectiveScope = scope != null ? scope : SeriesEditScope.THIS_OCCURRENCE;

        if (appointment.getSeries() == null || effectiveScope == SeriesEditScope.THIS_OCCURRENCE) {
            if (appointment.getSeries() == null) {
                publishDeleting(appointment);
                publishChanged(appointment, AppointmentChangeType.DELETED);
                appointmentRepository.delete(appointment);
                log.info(
                        "Deleted appointment {} in organization {}", appointmentId, organizationId);
            } else {
                Long seriesId = appointment.getSeries().getId();
                publishDeleting(appointment);
                publishChanged(appointment, AppointmentChangeType.DELETED);
                appointmentRepository.delete(appointment);
                deleteSeriesIfNoOccurrences(seriesId);
                log.info(
                        "Deleted appointment {} in organization {}", appointmentId, organizationId);
            }
            return;
        }

        AppointmentSeries series = appointment.getSeries();
        Long seriesId = series.getId();

        if (effectiveScope == SeriesEditScope.ENTIRE_SERIES) {
            List<Appointment> occurrences = appointmentRepository.findBySeries_Id(seriesId);
            for (Appointment occurrence : occurrences) {
                publishDeleting(occurrence);
                publishChanged(occurrence, AppointmentChangeType.DELETED);
            }
            appointmentRepository.deleteAll(occurrences);
            appointmentSeriesRepository.delete(series);
            log.info("Deleted appointment series {} in organization {}", seriesId, organizationId);
            return;
        }

        if (effectiveScope == SeriesEditScope.THIS_AND_FUTURE) {
            series.setSeriesEnd(appointment.getOccurrenceStart().minusSeconds(1));
            appointmentSeriesRepository.save(series);
            List<Appointment> future =
                    appointmentRepository.findBySeries_IdAndOccurrenceStartGreaterThanEqual(
                            seriesId, appointment.getOccurrenceStart());
            for (Appointment occurrence : future) {
                publishDeleting(occurrence);
                publishChanged(occurrence, AppointmentChangeType.DELETED);
            }
            appointmentRepository.deleteAll(future);
            deleteSeriesIfNoOccurrences(seriesId);
            log.info(
                    "Deleted future appointments of series {} in organization {} from occurrence {}",
                    seriesId,
                    organizationId,
                    appointmentId);
        }
    }

    private void deleteSeriesIfNoOccurrences(Long seriesId) {
        if (seriesId == null || appointmentRepository.countBySeries_Id(seriesId) > 0) {
            return;
        }
        appointmentSeriesRepository.deleteById(seriesId);
    }

    private AppointmentDto createSeriesAppointment(
            Organization organization, CreateAppointmentRequest request) {
        RecurrenceRequest recurrence = request.getRecurrence();
        AppointmentSeries series =
                recurrenceService.createSeries(organization, request, recurrence);
        AppointmentSeries savedSeries = appointmentSeriesRepository.save(series);

        Instant materializationEnd =
                recurrenceService.resolveMaterializationEnd(
                        request.getStart(), recurrence.getSeriesEnd());
        List<Instant> occurrenceStarts =
                recurrenceService.expandOccurrenceStarts(
                        recurrence.getRecurrenceRule(),
                        request.getStart(),
                        materializationEnd,
                        recurrence.getTimezone());

        List<Appointment> occurrences =
                recurrenceService.materializeOccurrences(savedSeries, request, occurrenceStarts);
        if (occurrences.isEmpty()) {
            throw new AppointmentValidationException("No occurrences generated for series");
        }
        // One signal for the series; clients invalidate the full appointment list once.
        publishChanged(occurrences.getFirst(), AppointmentChangeType.CREATED);
        log.info(
                "Created appointment series {} in organization {}",
                savedSeries.getId(),
                organization.getId());
        return appointmentMapper.toDto(occurrences.getFirst());
    }

    private void updateEntireSeries(Appointment appointment, UpdateAppointmentRequest request) {
        AppointmentSeries series = appointment.getSeries();
        if (series == null) {
            return;
        }
        Instant anchorStart = appointment.getStart();
        Instant anchorEnd = appointment.getEnd();
        Instant targetAnchorStart = request.getStart() != null ? request.getStart() : anchorStart;
        Instant targetAnchorEnd = request.getEnd() != null ? request.getEnd() : anchorEnd;
        Duration startShift = Duration.between(anchorStart, targetAnchorStart);
        Duration endShift = Duration.between(anchorEnd, targetAnchorEnd);
        Duration targetDuration = Duration.between(targetAnchorStart, targetAnchorEnd);

        if (request.getName() != null) {
            series.setName(request.getName());
        }
        if (request.getType() != null) {
            series.setType(request.getType());
        }
        if (request.getLocation() != null) {
            series.setLocation(request.getLocation());
        }
        if (request.getDescription() != null) {
            series.setDescription(request.getDescription());
        }
        if (request.getResponseMode() != null) {
            series.setResponseMode(request.getResponseMode());
        }
        if (request.getResponseDeadlineMinutesBefore() != null) {
            series.setResponseDeadlineMinutesBefore(request.getResponseDeadlineMinutesBefore());
        }
        if (request.getAudienceScope() != null) {
            series.setAudienceScope(request.getAudienceScope());
        }
        if (request.getAudienceGroupIds() != null) {
            series.setAudienceGroupIds(new ArrayList<>(request.getAudienceGroupIds()));
        }
        if (request.getStart() != null) {
            series.setSeriesStart(series.getSeriesStart().plus(startShift));
        }
        if (request.getResponseMode() == AppointmentResponseMode.NONE) {
            series.setResponseDeadlineMinutesBefore(0);
        }

        if (request.getRecurrence() != null) {
            RecurrenceRequest recurrence = request.getRecurrence();
            series.setRecurrenceRule(recurrence.getRecurrenceRule());
            series.setSeriesEnd(recurrence.getSeriesEnd());
            if (recurrence.getTimezone() != null && !recurrence.getTimezone().isBlank()) {
                series.setTimezone(recurrence.getTimezone());
            }
            // Keep the real series start (time-shifted above). Never rebase it onto the
            // edited occurrence - that drops earlier appointments when editing "entire series".
            appointmentSeriesRepository.save(series);
            syncSeriesOccurrences(
                    series,
                    request,
                    targetAnchorStart,
                    targetAnchorEnd,
                    startShift,
                    endShift,
                    targetDuration);
            return;
        }

        appointmentSeriesRepository.save(series);

        List<Appointment> occurrences = appointmentRepository.findBySeries_Id(series.getId());
        for (Appointment occurrence : occurrences) {
            if (!Boolean.TRUE.equals(occurrence.getIsException())) {
                applyUpdateWithoutTime(occurrence, request);
                applySeriesTimeShift(occurrence, request, startShift, endShift, targetDuration);
            }
        }
        appointmentRepository.saveAll(occurrences);
    }

    /**
     * Align materialized occurrences with the series rule and end: keep matching anchors
     * (preserving RSVPs), drop anything outside the new schedule (including exceptions past the new
     * end), and create only missing starts.
     */
    private void syncSeriesOccurrences(
            AppointmentSeries series,
            UpdateAppointmentRequest request,
            Instant anchorStart,
            Instant anchorEnd,
            Duration startShift,
            Duration endShift,
            Duration targetDuration) {
        RecurrenceRequest recurrence = request.getRecurrence();
        Instant materializationEnd =
                recurrenceService.resolveMaterializationEnd(
                        series.getSeriesStart(), recurrence.getSeriesEnd());
        List<Instant> desiredStarts =
                recurrenceService.expandOccurrenceStarts(
                        recurrence.getRecurrenceRule(),
                        series.getSeriesStart(),
                        materializationEnd,
                        series.getTimezone());
        Set<Instant> desiredSet = new HashSet<>(desiredStarts);

        List<Appointment> existing = appointmentRepository.findBySeries_Id(series.getId());
        List<Appointment> toDelete = new ArrayList<>();
        List<Appointment> toUpdate = new ArrayList<>();
        Set<Instant> retainedStarts = new HashSet<>();

        for (Appointment occurrence : existing) {
            Instant occurrenceStart = occurrence.getOccurrenceStart();
            if (!desiredSet.contains(occurrenceStart)) {
                toDelete.add(occurrence);
                continue;
            }
            retainedStarts.add(occurrenceStart);
            if (!Boolean.TRUE.equals(occurrence.getIsException())) {
                applyUpdateWithoutTime(occurrence, request);
                applySeriesTimeShift(occurrence, request, startShift, endShift, targetDuration);
                toUpdate.add(occurrence);
            }
        }

        if (!toDelete.isEmpty()) {
            appointmentRepository.deleteAll(toDelete);
            appointmentRepository.flush();
        }
        if (!toUpdate.isEmpty()) {
            appointmentRepository.saveAll(toUpdate);
        }

        List<Instant> missingStarts =
                desiredStarts.stream().filter(start -> !retainedStarts.contains(start)).toList();
        if (desiredStarts.isEmpty()) {
            throw new AppointmentValidationException("No occurrences generated for series");
        }
        if (!missingStarts.isEmpty()) {
            CreateAppointmentRequest template =
                    buildSeriesTemplateFromUpdate(series, request, anchorStart, anchorEnd);
            recurrenceService.materializeOccurrences(series, template, missingStarts);
        }
    }

    private CreateAppointmentRequest buildSeriesTemplateFromUpdate(
            AppointmentSeries series,
            UpdateAppointmentRequest request,
            Instant anchorStart,
            Instant anchorEnd) {
        CreateAppointmentRequest template = new CreateAppointmentRequest();
        template.setName(request.getName() != null ? request.getName() : series.getName());
        template.setType(request.getType() != null ? request.getType() : series.getType());
        template.setStart(anchorStart);
        template.setEnd(anchorEnd);
        template.setLocation(
                request.getLocation() != null ? request.getLocation() : series.getLocation());
        template.setDescription(
                request.getDescription() != null
                        ? request.getDescription()
                        : series.getDescription());
        template.setResponseMode(
                request.getResponseMode() != null
                        ? request.getResponseMode()
                        : series.getResponseMode());
        template.setResponseDeadlineMinutesBefore(
                request.getResponseDeadlineMinutesBefore() != null
                        ? request.getResponseDeadlineMinutesBefore()
                        : series.getResponseDeadlineMinutesBefore());
        template.setAudienceScope(
                request.getAudienceScope() != null
                        ? request.getAudienceScope()
                        : series.getAudienceScope());
        template.setAudienceGroupIds(
                request.getAudienceGroupIds() != null
                        ? request.getAudienceGroupIds()
                        : series.getAudienceGroupIds());
        return template;
    }

    private void splitSeriesAndUpdateFuture(
            Appointment appointment, UpdateAppointmentRequest request) {
        AppointmentSeries oldSeries = appointment.getSeries();
        if (oldSeries == null) {
            return;
        }
        Instant previousSeriesEnd = oldSeries.getSeriesEnd();
        oldSeries.setSeriesEnd(appointment.getOccurrenceStart().minusSeconds(1));
        appointmentSeriesRepository.save(oldSeries);

        List<Appointment> future =
                appointmentRepository.findBySeries_IdAndOccurrenceStartGreaterThanEqual(
                        oldSeries.getId(), appointment.getOccurrenceStart());
        if (future.isEmpty()) {
            return;
        }

        Instant anchorStart = appointment.getStart();
        Instant anchorEnd = appointment.getEnd();
        Instant targetAnchorStart = request.getStart() != null ? request.getStart() : anchorStart;
        Instant targetAnchorEnd = request.getEnd() != null ? request.getEnd() : anchorEnd;
        Duration startShift = Duration.between(anchorStart, targetAnchorStart);
        Duration endShift = Duration.between(anchorEnd, targetAnchorEnd);
        Duration targetDuration = Duration.between(targetAnchorStart, targetAnchorEnd);

        CreateAppointmentRequest template = new CreateAppointmentRequest();
        template.setName(request.getName() != null ? request.getName() : appointment.getName());
        template.setType(request.getType() != null ? request.getType() : appointment.getType());
        template.setStart(targetAnchorStart);
        template.setEnd(targetAnchorEnd);
        template.setLocation(
                request.getLocation() != null ? request.getLocation() : appointment.getLocation());
        template.setDescription(
                request.getDescription() != null
                        ? request.getDescription()
                        : appointment.getDescription());
        template.setResponseMode(
                request.getResponseMode() != null
                        ? request.getResponseMode()
                        : appointment.getResponseMode());
        template.setResponseDeadlineMinutesBefore(
                request.getResponseDeadlineMinutesBefore() != null
                        ? request.getResponseDeadlineMinutesBefore()
                        : appointment.getResponseDeadlineMinutesBefore());
        template.setAudienceScope(
                request.getAudienceScope() != null
                        ? request.getAudienceScope()
                        : appointment.getAudienceScope());
        template.setAudienceGroupIds(
                request.getAudienceGroupIds() != null
                        ? request.getAudienceGroupIds()
                        : appointment.getAudienceGroupIds());

        RecurrenceRequest recurrence = new RecurrenceRequest();
        recurrence.setRecurrenceRule(oldSeries.getRecurrenceRule());
        recurrence.setSeriesEnd(previousSeriesEnd);
        recurrence.setTimezone(oldSeries.getTimezone());
        template.setRecurrence(recurrence);

        AppointmentSeries newSeries =
                recurrenceService.createSeries(appointment.getOrganization(), template, recurrence);
        newSeries.setSeriesStart(targetAnchorStart);
        AppointmentSeries savedNewSeries = appointmentSeriesRepository.save(newSeries);

        for (Appointment futureOccurrence : future) {
            futureOccurrence.setSeries(savedNewSeries);
            futureOccurrence.setIsException(false);
            applyUpdateWithoutTime(futureOccurrence, request);
            applySeriesTimeShift(futureOccurrence, request, startShift, endShift, targetDuration);
        }
        appointmentRepository.saveAll(future);
    }

    private Appointment buildAppointment(
            Organization organization, CreateAppointmentRequest request) {
        Appointment appointment =
                Appointment.builder()
                        .tenantId(organization.getTenantId())
                        .organization(organization)
                        .series(null)
                        .occurrenceStart(request.getStart())
                        .name(request.getName())
                        .type(request.getType())
                        .start(request.getStart())
                        .end(request.getEnd())
                        .location(request.getLocation())
                        .description(request.getDescription())
                        .responseMode(request.getResponseMode())
                        .responseDeadlineMinutesBefore(
                                AppointmentResponseDeadlineUtil.normalizeMinutes(
                                        request.getResponseMode(),
                                        request.getResponseDeadlineMinutesBefore()))
                        .audienceScope(request.getAudienceScope())
                        .archived(false)
                        .build();
        appointment.replaceAudienceGroupIds(request.getAudienceGroupIds());
        return appointment;
    }

    private void applyUpdate(Appointment appointment, UpdateAppointmentRequest request) {
        applyUpdateWithoutTime(appointment, request);
        if (request.getStart() != null) {
            appointment.setStart(request.getStart());
            // For series occurrences, occurrenceStart remains the original recurrence anchor.
            if (appointment.getSeries() == null) {
                appointment.setOccurrenceStart(request.getStart());
            }
        }
        if (request.getEnd() != null) {
            appointment.setEnd(request.getEnd());
        }
    }

    private void applyUpdateWithoutTime(Appointment appointment, UpdateAppointmentRequest request) {
        if (request.getName() != null) {
            appointment.setName(request.getName());
        }
        if (request.getType() != null) {
            appointment.setType(request.getType());
        }
        if (request.getLocation() != null) {
            appointment.setLocation(request.getLocation());
        }
        if (request.getDescription() != null) {
            appointment.setDescription(request.getDescription());
        }
        if (request.getResponseMode() != null) {
            appointment.setResponseMode(request.getResponseMode());
        }
        if (request.getResponseDeadlineMinutesBefore() != null) {
            appointment.setResponseDeadlineMinutesBefore(
                    request.getResponseDeadlineMinutesBefore());
        }
        if (request.getAudienceScope() != null) {
            appointment.setAudienceScope(request.getAudienceScope());
        }
        if (request.getAudienceGroupIds() != null) {
            appointment.setAudienceGroupIds(new ArrayList<>(request.getAudienceGroupIds()));
        }
        if (request.getResponseMode() == AppointmentResponseMode.NONE) {
            appointment.setResponseDeadlineMinutesBefore(0);
        }
    }

    private void applySeriesTimeShift(
            Appointment appointment,
            UpdateAppointmentRequest request,
            Duration startShift,
            Duration endShift,
            Duration targetDuration) {
        if (request.getStart() == null && request.getEnd() == null) {
            return;
        }

        Instant nextStart = appointment.getStart();
        Instant nextEnd = appointment.getEnd();

        if (request.getStart() != null) {
            nextStart = nextStart.plus(startShift);
        }

        if (request.getEnd() != null) {
            if (request.getStart() != null) {
                nextEnd = nextStart.plus(targetDuration);
            } else {
                nextEnd = nextEnd.plus(endShift);
            }
        } else {
            nextEnd = nextEnd.plus(startShift);
        }

        if (!nextEnd.isAfter(nextStart)) {
            throw new AppointmentValidationException("End must be after start");
        }

        appointment.setStart(nextStart);
        appointment.setEnd(nextEnd);
        appointment.setOccurrenceStart(nextStart);
    }

    private long countUnread(Long userId, List<Appointment> visibleAppointments) {
        Instant listHorizonEnd =
                plusMonths(Instant.now(), AppointmentRecurrenceService.SERIES_HORIZON_MONTHS);
        return countUnread(userId, visibleAppointments, listHorizonEnd);
    }

    private long countUnread(
            Long userId, List<Appointment> visibleAppointments, Instant listHorizonEnd) {
        List<Appointment> candidates =
                visibleAppointments.stream()
                        .filter(a -> a.getStart().isBefore(listHorizonEnd))
                        .toList();
        if (candidates.isEmpty()) {
            return 0;
        }
        List<Long> appointmentIds = candidates.stream().map(Appointment::getId).toList();
        Set<Long> readIds =
                new HashSet<>(
                        appointmentReadRepository.findAppointmentIdsByUserIdAndAppointmentIdIn(
                                userId, appointmentIds));
        return candidates.stream().filter(a -> !readIds.contains(a.getId())).count();
    }

    private List<AppointmentDto> enrichUnread(Long userId, List<AppointmentDto> dtos) {
        if (dtos.isEmpty()) {
            return dtos;
        }
        List<Long> appointmentIds = dtos.stream().map(AppointmentDto::getId).toList();
        Set<Long> readIds =
                new HashSet<>(
                        appointmentReadRepository.findAppointmentIdsByUserIdAndAppointmentIdIn(
                                userId, appointmentIds));
        for (AppointmentDto dto : dtos) {
            dto.setUnread(!readIds.contains(dto.getId()));
        }
        return dtos;
    }

    @Transactional
    public void validateAndArchiveAfterEventPromote(
            Long organizationId,
            Long appointmentId,
            EventType createdEventType,
            Instant eventStart,
            Instant eventEnd) {

        Long userId = tokenService.requireUserId();
        if (!organizationAccessService.hasOrganizationPermission(
                organizationId, PermissionType.APPOINTMENT_WRITE)) {
            throw new AppointmentAccessDeniedException(
                    "User does not have appointment write access");
        }
        audienceService.ensureUserCanAccess(userId, appointmentId);

        Appointment appointment = findAppointment(organizationId, appointmentId);
        if (appointment.isArchived()) {
            throw new AppointmentValidationException("Appointment is already archived");
        }
        Instant now = Instant.now();
        if (appointment.getStart().isAfter(now)) {
            throw new AppointmentValidationException(
                    "An event can only be created from an appointment after the appointment has started");
        }
        if (!appointmentTypeMatchesEventType(appointment.getType(), createdEventType)) {
            throw new AppointmentValidationException("Appointment type does not match event type");
        }
        if (!appointment.getStart().equals(eventStart) || !appointment.getEnd().equals(eventEnd)) {
            throw new AppointmentValidationException(
                    "Event start and end must match the source appointment");
        }
        LifecycleFields.archive(appointment, now);
        appointmentRepository.save(appointment);
        log.info(
                "Archived appointment {} in organization {} after event promotion",
                appointmentId,
                organizationId);
    }

    private Appointment findAppointment(Long organizationId, Long appointmentId) {
        return appointmentRepository
                .findByIdAndOrganization_IdAndArchivedIsFalse(appointmentId, organizationId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
    }

    private void validateOrganizationIdConsistency(
            Long expectedOrganizationId, Long actualOrganizationId) {
        if (!expectedOrganizationId.equals(actualOrganizationId)) {
            throw new AppointmentValidationException(
                    "Organization ID in request does not match the path parameter");
        }
    }

    private void publishDeleting(Appointment appointment) {
        if (appointment == null || appointment.getId() == null) {
            return;
        }
        Long organizationId =
                appointment.getOrganization() != null
                        ? appointment.getOrganization().getId()
                        : null;
        eventPublisher.publishEvent(
                new AppointmentDeletingEvent(
                        appointment.getTenantId(), organizationId, appointment.getId()));
    }

    private void publishChanged(Appointment appointment, AppointmentChangeType changeType) {
        if (appointment == null || appointment.getId() == null) {
            return;
        }
        Long organizationId =
                appointment.getOrganization() != null
                        ? appointment.getOrganization().getId()
                        : null;
        eventPublisher.publishEvent(
                new AppointmentChangedEvent(
                        appointment.getTenantId(),
                        organizationId,
                        appointment.getId(),
                        changeType));
    }
}
