/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.emberone.appointment.domain.*;
import com.emberone.appointment.dto.AppointmentDto;
import com.emberone.appointment.dto.CreateAppointmentRequest;
import com.emberone.appointment.dto.RecurrenceRequest;
import com.emberone.appointment.dto.UpdateAppointmentRequest;
import com.emberone.appointment.event.AppointmentChangeType;
import com.emberone.appointment.event.AppointmentChangedEvent;
import com.emberone.appointment.event.AppointmentDeletingEvent;
import com.emberone.appointment.exception.AppointmentAccessDeniedException;
import com.emberone.appointment.exception.AppointmentNotFoundException;
import com.emberone.appointment.exception.AppointmentValidationException;
import com.emberone.appointment.list.AppointmentListFilter;
import com.emberone.appointment.mapper.AppointmentDtoAssembler;
import com.emberone.appointment.mapper.AppointmentMapper;
import com.emberone.appointment.repository.AppointmentReadRepository;
import com.emberone.appointment.repository.AppointmentRepository;
import com.emberone.appointment.repository.AppointmentSeriesRepository;
import com.emberone.event.domain.EventType;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.security.OrganizationAccessService;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock AppointmentRepository appointmentRepository;
    @Mock AppointmentSeriesRepository seriesRepository;
    @Mock AppointmentReadRepository readRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock UserOrganizationGroupMembershipRepository groupMembershipRepository;
    @Mock AppointmentAudienceService audienceService;
    @Mock OrganizationAccessService organizationAccessService;
    @Mock AppointmentRecurrenceService recurrenceService;
    @Mock AppointmentMapper mapper;
    @Mock AppointmentDtoAssembler appointmentDtoAssembler;
    @Mock AppointmentRequestValidator validator;
    @Mock TokenService tokenService;
    @Mock ApplicationEventPublisher eventPublisher;

    private AppointmentService service;

    @BeforeEach
    void setUp() {
        service =
                new AppointmentService(
                        appointmentRepository,
                        seriesRepository,
                        readRepository,
                        organizationRepository,
                        membershipRepository,
                        groupMembershipRepository,
                        audienceService,
                        organizationAccessService,
                        recurrenceService,
                        mapper,
                        appointmentDtoAssembler,
                        validator,
                        tokenService,
                        eventPublisher);
        lenient()
                .when(
                        organizationAccessService.hasOrganizationPermission(
                                anyLong(), eq(PermissionType.APPOINTMENT_WRITE)))
                .thenReturn(true);
    }

    @Test
    void createSingleAppointmentValidatesOrganizationConsistencyAndPersistsAudience() {
        CreateAppointmentRequest request = request();
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(appointmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDto(any())).thenReturn(AppointmentDto.builder().build());

        service.createAppointment(10L, request);

        verify(validator).normalizeAndValidateCreate(request);
        var captor = org.mockito.ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getOccurrenceStart()).isEqualTo(request.getStart());
        assertThat(captor.getValue().getAudienceGroupIds()).containsExactly(20L);

        request.setOrganizationId(99L);
        assertThatThrownBy(() -> service.createAppointment(10L, request))
                .isInstanceOf(AppointmentValidationException.class);
    }

    @Test
    void createSeriesDelegatesExpansionAndRequiresOccurrence() {
        CreateAppointmentRequest request = request();
        RecurrenceRequest recurrence = new RecurrenceRequest();
        recurrence.setRecurrenceRule("FREQ=WEEKLY");
        request.setRecurrence(recurrence);
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        AppointmentSeries series = AppointmentSeries.builder().id(3L).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(recurrenceService.createSeries(organization, request, recurrence)).thenReturn(series);
        when(seriesRepository.save(series)).thenReturn(series);
        when(recurrenceService.resolveMaterializationEnd(request.getStart(), null))
                .thenReturn(request.getStart().atZone(ZoneOffset.UTC).plusMonths(12).toInstant());
        when(recurrenceService.expandOccurrenceStarts(any(), any(), any(), any()))
                .thenReturn(List.of(request.getStart()));
        Appointment first = Appointment.builder().id(5L).build();
        when(recurrenceService.materializeOccurrences(series, request, List.of(request.getStart())))
                .thenReturn(List.of(first));
        when(mapper.toDto(first)).thenReturn(AppointmentDto.builder().id(5L).build());

        assertThat(service.createAppointment(10L, request).getId()).isEqualTo(5L);

        when(recurrenceService.materializeOccurrences(series, request, List.of(request.getStart())))
                .thenReturn(List.of());
        assertThatThrownBy(() -> service.createAppointment(10L, request))
                .isInstanceOf(AppointmentValidationException.class)
                .hasMessageContaining("No occurrences");
    }

    @Test
    void updateRejectsArchivedAppointmentBeforeSaving() {
        Appointment appointment = Appointment.builder().id(5L).archived(true).build();
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.of(appointment));

        assertThatThrownBy(
                        () ->
                                service.updateAppointment(
                                        10L, 5L, new UpdateAppointmentRequest(), null))
                .isInstanceOf(AppointmentValidationException.class)
                .hasMessageContaining("archived");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void deletingSingleSeriesOccurrenceDeletesEmptySeries() {
        Organization organization = Organization.builder().id(10L).build();
        AppointmentSeries series = AppointmentSeries.builder().id(3L).build();
        Appointment appointment =
                Appointment.builder()
                        .id(5L)
                        .tenantId(7L)
                        .organization(organization)
                        .series(series)
                        .build();
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.of(appointment));
        when(appointmentRepository.countBySeries_Id(3L)).thenReturn(0L);

        service.deleteAppointment(10L, 5L, SeriesEditScope.THIS_OCCURRENCE);

        verify(eventPublisher).publishEvent(new AppointmentDeletingEvent(7L, 10L, 5L));
        verify(eventPublisher)
                .publishEvent(
                        new AppointmentChangedEvent(7L, 10L, 5L, AppointmentChangeType.DELETED));
        verify(appointmentRepository).delete(appointment);
        verify(seriesRepository).deleteById(3L);
    }

    @Test
    void listReturnsPagedVisibleAppointments() {
        Appointment visible = Appointment.builder().id(1L).archived(false).build();
        when(tokenService.requireUserId()).thenReturn(3L);
        when(groupMembershipRepository.findActiveGroupIdsForUserInOrganization(
                        eq(3L), eq(10L), any()))
                .thenReturn(List.of());
        Page<Appointment> page = new PageImpl<>(List.of(visible), PageRequest.of(0, 50), 1);
        when(appointmentRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(page);
        when(appointmentRepository.findWithOrganizationAndAudienceByIdIn(List.of(1L)))
                .thenReturn(List.of(visible));
        when(appointmentDtoAssembler.toDtos(List.of(visible)))
                .thenReturn(List.of(AppointmentDto.builder().id(1L).build()));
        when(appointmentDtoAssembler.toPageDto(any(), any()))
                .thenAnswer(
                        invocation ->
                                com.emberone.appointment.dto.AppointmentPageDto.builder()
                                        .content(invocation.getArgument(1))
                                        .page(0)
                                        .size(50)
                                        .totalElements(1)
                                        .totalPages(1)
                                        .hasNext(false)
                                        .build());

        assertThat(
                        service.listAppointments(
                                        10L,
                                        new ListQuery(0, null, null, null),
                                        AppointmentListFilter.parseOrg(
                                                List.of("UPCOMING"), null, null, null))
                                .getContent())
                .extracting(AppointmentDto::getId)
                .containsExactly(1L);
    }

    @Test
    void updateSingleOccurrenceMarksSeriesExceptionAndPersistsAudienceChanges() {
        AppointmentSeries series = AppointmentSeries.builder().id(3L).build();
        Appointment appointment =
                Appointment.builder().id(5L).series(series).archived(false).build();
        UpdateAppointmentRequest request = new UpdateAppointmentRequest();
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(appointment)).thenReturn(appointment);

        service.updateAppointment(10L, 5L, request, SeriesEditScope.THIS_OCCURRENCE);

        assertThat(appointment.getIsException()).isTrue();
        verify(validator).normalizeAndValidateUpdate(10L, appointment, request);
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void deletingEntireSeriesDeletesOccurrencesThenSeries() {
        Organization organization = Organization.builder().id(10L).build();
        AppointmentSeries series = AppointmentSeries.builder().id(3L).build();
        Appointment selected =
                Appointment.builder()
                        .id(5L)
                        .tenantId(7L)
                        .organization(organization)
                        .series(series)
                        .build();
        Appointment sibling =
                Appointment.builder()
                        .id(6L)
                        .tenantId(7L)
                        .organization(organization)
                        .series(series)
                        .build();
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.of(selected));
        when(appointmentRepository.findBySeries_Id(3L)).thenReturn(List.of(selected, sibling));

        service.deleteAppointment(10L, 5L, SeriesEditScope.ENTIRE_SERIES);

        verify(eventPublisher).publishEvent(new AppointmentDeletingEvent(7L, 10L, 5L));
        verify(eventPublisher).publishEvent(new AppointmentDeletingEvent(7L, 10L, 6L));
        verify(eventPublisher)
                .publishEvent(
                        new AppointmentChangedEvent(7L, 10L, 5L, AppointmentChangeType.DELETED));
        verify(eventPublisher)
                .publishEvent(
                        new AppointmentChangedEvent(7L, 10L, 6L, AppointmentChangeType.DELETED));
        verify(appointmentRepository).deleteAll(List.of(selected, sibling));
        verify(seriesRepository).delete(series);
    }

    @Test
    void getAppointmentLoadsVisibleAppointmentAndMapsDto() {
        Appointment appointment = Appointment.builder().id(5L).archived(false).build();
        AppointmentDto dto = AppointmentDto.builder().id(5L).build();
        when(tokenService.requireUserId()).thenReturn(3L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.of(appointment));
        when(mapper.toDto(appointment)).thenReturn(dto);

        assertThat(service.getAppointment(10L, 5L)).isSameAs(dto);
        verify(audienceService).ensureUserCanAccess(3L, 5L);
    }

    @Test
    void getAppointmentThrowsWhenMissing() {
        when(tokenService.requireUserId()).thenReturn(3L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAppointment(10L, 5L))
                .isInstanceOf(AppointmentNotFoundException.class);
    }

    @Test
    void promoteArchivesMatchingStartedAppointment() {
        Instant start = Instant.now().minus(Duration.ofHours(1));
        Instant end = start.plus(Duration.ofHours(2));
        Appointment appointment =
                Appointment.builder()
                        .id(5L)
                        .type(AppointmentType.SERVICE)
                        .start(start)
                        .end(end)
                        .archived(false)
                        .build();
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(appointment)).thenReturn(appointment);

        service.validateAndArchiveAfterEventPromote(10L, 5L, EventType.SERVICE, start, end);

        assertThat(appointment.isArchived()).isTrue();
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void listUpcomingAcrossOrganizationsAggregatesVisibleAppointments() {
        Organization organization = Organization.builder().id(10L).build();
        UserOrganizationMembership membership =
                UserOrganizationMembership.builder()
                        .organization(organization)
                        .startDate(Instant.now())
                        .build();
        Appointment upcoming =
                Appointment.builder()
                        .id(1L)
                        .archived(false)
                        .start(Instant.now().plus(Duration.ofDays(1)))
                        .build();
        when(tokenService.requireUserId()).thenReturn(3L);
        when(membershipRepository.findActiveByUser_Id(eq(3L), any(Instant.class)))
                .thenReturn(List.of(membership));
        when(appointmentRepository.findUpcomingByOrganizationId(eq(10L), any()))
                .thenReturn(List.of(upcoming));
        when(audienceService.filterAppointmentsVisibleToUser(3L, List.of(upcoming)))
                .thenReturn(List.of(upcoming));
        when(mapper.toDto(upcoming))
                .thenReturn(AppointmentDto.builder().id(1L).start(upcoming.getStart()).build());

        assertThat(service.listUpcomingAcrossOrganizations(5).getContent())
                .extracting(AppointmentDto::getId)
                .containsExactly(1L);
        assertThat(service.listUpcomingAcrossOrganizations(5).getTotalCount()).isEqualTo(1);
    }

    @Test
    void updateEntireSeriesShiftsNonExceptionOccurrencesAndUpdatesSeriesMetadata() {
        Instant anchorStart = LocalDateTime.of(2026, 7, 1, 18, 0).toInstant(ZoneOffset.UTC);
        Instant anchorEnd = anchorStart.plus(Duration.ofHours(2));
        AppointmentSeries series =
                AppointmentSeries.builder()
                        .id(3L)
                        .name("Old name")
                        .seriesStart(anchorStart)
                        .recurrenceRule("FREQ=WEEKLY")
                        .build();
        Appointment regular = seriesOccurrence(5L, series, anchorStart, anchorEnd, false);
        Appointment exception =
                seriesOccurrence(
                        6L,
                        series,
                        anchorStart.plus(Duration.ofDays(7L * (1))),
                        anchorEnd.plus(Duration.ofDays(7L * (1))),
                        true);
        Instant exceptionStart = exception.getStart();
        Instant exceptionEnd = exception.getEnd();
        UpdateAppointmentRequest request = new UpdateAppointmentRequest();
        request.setName("New name");
        request.setStart(anchorStart.plus(Duration.ofHours(1)));
        request.setEnd(anchorEnd.plus(Duration.ofHours(1)));
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.of(regular));
        when(appointmentRepository.findBySeries_Id(3L)).thenReturn(List.of(regular, exception));
        when(mapper.toDto(regular)).thenReturn(AppointmentDto.builder().id(5L).build());

        service.updateAppointment(10L, 5L, request, SeriesEditScope.ENTIRE_SERIES);

        assertThat(series.getName()).isEqualTo("New name");
        assertThat(series.getSeriesStart()).isEqualTo(anchorStart.plus(Duration.ofHours(1)));
        assertThat(regular.getStart()).isEqualTo(anchorStart.plus(Duration.ofHours(1)));
        assertThat(regular.getEnd()).isEqualTo(anchorEnd.plus(Duration.ofHours(1)));
        assertThat(exception.getStart()).isEqualTo(exceptionStart);
        assertThat(exception.getEnd()).isEqualTo(exceptionEnd);
        verify(seriesRepository).save(series);
        verify(appointmentRepository).saveAll(List.of(regular, exception));
    }

    @Test
    void updateEntireSeriesWithRecurrenceChangeSyncsOccurrencesFromSeriesStart() {
        Instant seriesStart = LocalDateTime.of(2026, 7, 1, 18, 0).toInstant(ZoneOffset.UTC);
        Instant seriesEndTime = seriesStart.plus(Duration.ofHours(2));
        Instant editedStart = seriesStart.plus(Duration.ofDays(7L * (2)));
        Instant editedEnd = editedStart.plus(Duration.ofHours(2));
        AppointmentSeries series =
                AppointmentSeries.builder()
                        .id(3L)
                        .seriesStart(seriesStart)
                        .recurrenceRule("FREQ=WEEKLY")
                        .timezone("Europe/Berlin")
                        .name("Training")
                        .type(AppointmentType.SERVICE)
                        .responseMode(AppointmentResponseMode.YES_NO)
                        .audienceScope(AudienceScope.SELECTED_GROUPS)
                        .build();
        Appointment first = seriesOccurrence(5L, series, seriesStart, seriesEndTime, false);
        Appointment middle = seriesOccurrence(6L, series, editedStart, editedEnd, false);
        Appointment pastEnd =
                seriesOccurrence(
                        7L,
                        series,
                        seriesStart.atZone(ZoneOffset.UTC).plusMonths(2).toInstant(),
                        seriesEndTime.atZone(ZoneOffset.UTC).plusMonths(2).toInstant(),
                        true);
        RecurrenceRequest recurrence = new RecurrenceRequest();
        recurrence.setRecurrenceRule("FREQ=WEEKLY");
        Instant newSeriesEnd = seriesStart.plus(Duration.ofDays(7L * (3)));
        recurrence.setSeriesEnd(newSeriesEnd);
        UpdateAppointmentRequest request = new UpdateAppointmentRequest();
        request.setRecurrence(recurrence);
        request.setStart(editedStart);
        request.setEnd(editedEnd);
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(6L, 10L))
                .thenReturn(Optional.of(middle));
        when(appointmentRepository.findBySeries_Id(3L)).thenReturn(List.of(first, middle, pastEnd));
        when(recurrenceService.resolveMaterializationEnd(seriesStart, newSeriesEnd))
                .thenReturn(newSeriesEnd);
        when(recurrenceService.expandOccurrenceStarts(
                        "FREQ=WEEKLY", seriesStart, newSeriesEnd, "Europe/Berlin"))
                .thenReturn(
                        List.of(
                                seriesStart,
                                seriesStart.plus(Duration.ofDays(7L * (1))),
                                seriesStart.plus(Duration.ofDays(7L * (2))),
                                seriesStart.plus(Duration.ofDays(7L * (3)))));
        when(mapper.toDto(middle)).thenReturn(AppointmentDto.builder().id(6L).build());

        service.updateAppointment(10L, 6L, request, SeriesEditScope.ENTIRE_SERIES);

        assertThat(series.getSeriesStart()).isEqualTo(seriesStart);
        assertThat(series.getSeriesEnd()).isEqualTo(newSeriesEnd);
        verify(appointmentRepository).deleteAll(List.of(pastEnd));
        verify(appointmentRepository).flush();
        verify(appointmentRepository).saveAll(List.of(first, middle));
        ArgumentCaptor<CreateAppointmentRequest> templateCaptor =
                ArgumentCaptor.forClass(CreateAppointmentRequest.class);
        verify(recurrenceService)
                .materializeOccurrences(
                        eq(series),
                        templateCaptor.capture(),
                        eq(
                                List.of(
                                        seriesStart.plus(Duration.ofDays(7L * (1))),
                                        seriesStart.plus(Duration.ofDays(7L * (3))))));
        assertThat(templateCaptor.getValue().getStart()).isEqualTo(editedStart);
    }

    @Test
    void updateEntireSeriesWithPatternChangeDoesNotRebaseSeriesStartOntoEditedOccurrence() {
        Instant seriesStart = LocalDateTime.of(2026, 7, 1, 18, 0).toInstant(ZoneOffset.UTC);
        Instant seriesEndTime = seriesStart.plus(Duration.ofHours(2));
        Instant editedStart = seriesStart.plus(Duration.ofDays(7L * (3)));
        Instant editedEnd = editedStart.plus(Duration.ofHours(2));
        AppointmentSeries series =
                AppointmentSeries.builder()
                        .id(3L)
                        .seriesStart(seriesStart)
                        .recurrenceRule("FREQ=WEEKLY")
                        .timezone("Europe/Berlin")
                        .name("Training")
                        .type(AppointmentType.SERVICE)
                        .responseMode(AppointmentResponseMode.YES_NO)
                        .audienceScope(AudienceScope.SELECTED_GROUPS)
                        .build();
        Appointment selected = seriesOccurrence(5L, series, editedStart, editedEnd, false);
        RecurrenceRequest recurrence = new RecurrenceRequest();
        recurrence.setRecurrenceRule("FREQ=DAILY");
        recurrence.setSeriesEnd(editedStart.plus(Duration.ofDays(2)));
        UpdateAppointmentRequest request = new UpdateAppointmentRequest();
        request.setRecurrence(recurrence);
        request.setStart(editedStart);
        request.setEnd(editedEnd);
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.of(selected));
        when(appointmentRepository.findBySeries_Id(3L)).thenReturn(List.of(selected));
        when(recurrenceService.resolveMaterializationEnd(seriesStart, recurrence.getSeriesEnd()))
                .thenReturn(recurrence.getSeriesEnd());
        when(recurrenceService.expandOccurrenceStarts(
                        "FREQ=DAILY", seriesStart, recurrence.getSeriesEnd(), "Europe/Berlin"))
                .thenReturn(
                        List.of(
                                seriesStart,
                                seriesStart.plus(Duration.ofDays(1)),
                                seriesStart.plus(Duration.ofDays(2))));
        when(mapper.toDto(selected)).thenReturn(AppointmentDto.builder().id(5L).build());

        service.updateAppointment(10L, 5L, request, SeriesEditScope.ENTIRE_SERIES);

        assertThat(series.getSeriesStart()).isEqualTo(seriesStart);
        verify(appointmentRepository).deleteAll(List.of(selected));
        verify(recurrenceService)
                .materializeOccurrences(
                        eq(series),
                        any(CreateAppointmentRequest.class),
                        eq(
                                List.of(
                                        seriesStart,
                                        seriesStart.plus(Duration.ofDays(1)),
                                        seriesStart.plus(Duration.ofDays(2)))));
    }

    @Test
    void updateEntireSeriesRejectsInvalidTimeShift() {
        Instant anchorStart = LocalDateTime.of(2026, 7, 1, 18, 0).toInstant(ZoneOffset.UTC);
        Instant anchorEnd = anchorStart.plus(Duration.ofHours(2));
        AppointmentSeries series =
                AppointmentSeries.builder()
                        .id(3L)
                        .seriesStart(anchorStart)
                        .recurrenceRule("FREQ=WEEKLY")
                        .build();
        Appointment occurrence = seriesOccurrence(5L, series, anchorStart, anchorEnd, false);
        UpdateAppointmentRequest request = new UpdateAppointmentRequest();
        request.setStart(anchorStart.plus(Duration.ofHours(3)));
        request.setEnd(anchorStart.plus(Duration.ofHours(1)));
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.of(occurrence));
        when(appointmentRepository.findBySeries_Id(3L)).thenReturn(List.of(occurrence));

        assertThatThrownBy(
                        () ->
                                service.updateAppointment(
                                        10L, 5L, request, SeriesEditScope.ENTIRE_SERIES))
                .isInstanceOf(AppointmentValidationException.class)
                .hasMessageContaining("End must be after start");
    }

    @Test
    void updateThisAndFutureSplitsSeriesAndReassignsFutureOccurrences() {
        Instant anchorStart = LocalDateTime.of(2026, 7, 1, 18, 0).toInstant(ZoneOffset.UTC);
        Instant anchorEnd = anchorStart.plus(Duration.ofHours(2));
        Instant previousSeriesEnd = anchorStart.atZone(ZoneOffset.UTC).plusMonths(3).toInstant();
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        AppointmentSeries oldSeries =
                AppointmentSeries.builder()
                        .id(3L)
                        .organization(organization)
                        .seriesStart(anchorStart)
                        .seriesEnd(previousSeriesEnd)
                        .recurrenceRule("FREQ=WEEKLY")
                        .timezone("Europe/Berlin")
                        .name("Training")
                        .type(AppointmentType.SERVICE)
                        .responseMode(AppointmentResponseMode.YES_NO)
                        .audienceScope(AudienceScope.SELECTED_GROUPS)
                        .build();
        Instant splitPoint = anchorStart.plus(Duration.ofDays(7L * (1)));
        Appointment selected =
                seriesOccurrence(
                        5L,
                        oldSeries,
                        splitPoint,
                        anchorEnd.plus(Duration.ofDays(7L * (1))),
                        false);
        selected.setOrganization(organization);
        Appointment future =
                seriesOccurrence(
                        6L,
                        oldSeries,
                        anchorStart.plus(Duration.ofDays(7L * (2))),
                        anchorEnd.plus(Duration.ofDays(7L * (2))),
                        false);
        future.setOrganization(organization);
        UpdateAppointmentRequest request = new UpdateAppointmentRequest();
        request.setName("Updated training");
        request.setStart(selected.getStart().plus(Duration.ofHours(1)));
        request.setEnd(selected.getEnd().plus(Duration.ofHours(1)));
        AppointmentSeries newSeries = AppointmentSeries.builder().id(4L).build();
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.of(selected));
        when(appointmentRepository.findBySeries_IdAndOccurrenceStartGreaterThanEqual(
                        3L, splitPoint))
                .thenReturn(List.of(selected, future));
        when(recurrenceService.createSeries(
                        eq(organization),
                        any(CreateAppointmentRequest.class),
                        any(RecurrenceRequest.class)))
                .thenReturn(newSeries);
        when(seriesRepository.save(oldSeries)).thenReturn(oldSeries);
        when(seriesRepository.save(newSeries)).thenReturn(newSeries);
        when(mapper.toDto(selected)).thenReturn(AppointmentDto.builder().id(5L).build());

        service.updateAppointment(10L, 5L, request, SeriesEditScope.THIS_AND_FUTURE);

        assertThat(oldSeries.getSeriesEnd()).isEqualTo(splitPoint.minusSeconds(1));
        assertThat(selected.getSeries()).isSameAs(newSeries);
        assertThat(future.getSeries()).isSameAs(newSeries);
        assertThat(selected.getIsException()).isFalse();
        assertThat(selected.getName()).isEqualTo("Updated training");
        verify(seriesRepository).save(oldSeries);
        verify(seriesRepository).save(newSeries);
        verify(appointmentRepository).saveAll(List.of(selected, future));
    }

    @Test
    void deleteThisAndFutureTruncatesSeriesAndDeletesFutureOccurrences() {
        Instant occurrenceStart = LocalDateTime.of(2026, 7, 8, 18, 0).toInstant(ZoneOffset.UTC);
        AppointmentSeries series = AppointmentSeries.builder().id(3L).build();
        Appointment selected =
                seriesOccurrence(
                        5L,
                        series,
                        occurrenceStart,
                        occurrenceStart.plus(Duration.ofHours(2)),
                        false);
        Appointment future =
                seriesOccurrence(
                        6L,
                        series,
                        occurrenceStart.plus(Duration.ofDays(7L * (1))),
                        occurrenceStart.plus(Duration.ofDays(7L * (1))).plus(Duration.ofHours(2)),
                        false);
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.of(selected));
        when(appointmentRepository.findBySeries_IdAndOccurrenceStartGreaterThanEqual(
                        3L, occurrenceStart))
                .thenReturn(List.of(selected, future));
        when(appointmentRepository.countBySeries_Id(3L)).thenReturn(1L);

        service.deleteAppointment(10L, 5L, SeriesEditScope.THIS_AND_FUTURE);

        assertThat(series.getSeriesEnd()).isEqualTo(occurrenceStart.minusSeconds(1));
        verify(seriesRepository).save(series);
        verify(appointmentRepository).deleteAll(List.of(selected, future));
        verify(seriesRepository, never()).deleteById(3L);
    }

    @Test
    void updateAppointmentThrowsWhenMissing() {
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.updateAppointment(
                                        10L, 5L, new UpdateAppointmentRequest(), null))
                .isInstanceOf(AppointmentNotFoundException.class);
    }

    @Test
    void deleteAppointmentThrowsWhenMissing() {
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteAppointment(10L, 5L, null))
                .isInstanceOf(AppointmentNotFoundException.class);
    }

    @Test
    void deleteSingleNonSeriesAppointmentDeletesOnlyOccurrence() {
        Appointment appointment = Appointment.builder().id(5L).series(null).build();
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.of(appointment));

        service.deleteAppointment(10L, 5L, null);

        verify(appointmentRepository).delete(appointment);
        verify(seriesRepository, never()).deleteById(any());
        verify(seriesRepository, never()).delete(any());
    }

    @Test
    void promoteRejectsWithoutAppointmentWritePermission() {
        Instant start = Instant.now().minus(Duration.ofHours(1));
        Instant end = start.plus(Duration.ofHours(2));
        when(tokenService.requireUserId()).thenReturn(1L);
        when(organizationAccessService.hasOrganizationPermission(
                        10L, PermissionType.APPOINTMENT_WRITE))
                .thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.validateAndArchiveAfterEventPromote(
                                        10L, 5L, EventType.SERVICE, start, end))
                .isInstanceOf(AppointmentAccessDeniedException.class)
                .hasMessageContaining("appointment write access");
        verify(audienceService, never()).ensureUserCanAccess(anyLong(), anyLong());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void promoteRejectsAlreadyArchivedAppointment() {
        Appointment appointment =
                Appointment.builder()
                        .id(5L)
                        .type(AppointmentType.SERVICE)
                        .start(Instant.now().minus(Duration.ofHours(1)))
                        .end(Instant.now().plus(Duration.ofHours(1)))
                        .archived(true)
                        .build();
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.of(appointment));

        assertThatThrownBy(
                        () ->
                                service.validateAndArchiveAfterEventPromote(
                                        10L,
                                        5L,
                                        EventType.SERVICE,
                                        appointment.getStart(),
                                        appointment.getEnd()))
                .isInstanceOf(AppointmentValidationException.class)
                .hasMessageContaining("already archived");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void promoteRejectsMismatchedEventType() {
        Instant start = Instant.now().minus(Duration.ofHours(1));
        Instant end = start.plus(Duration.ofHours(2));
        Appointment appointment =
                Appointment.builder()
                        .id(5L)
                        .type(AppointmentType.SERVICE)
                        .start(start)
                        .end(end)
                        .archived(false)
                        .build();
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.of(appointment));

        assertThatThrownBy(
                        () ->
                                service.validateAndArchiveAfterEventPromote(
                                        10L, 5L, EventType.OPERATION, start, end))
                .isInstanceOf(AppointmentValidationException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void promoteRejectsMismatchedEventTimes() {
        Instant start = Instant.now().minus(Duration.ofHours(1));
        Instant end = start.plus(Duration.ofHours(2));
        Appointment appointment =
                Appointment.builder()
                        .id(5L)
                        .type(AppointmentType.SERVICE)
                        .start(start)
                        .end(end)
                        .archived(false)
                        .build();
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.of(appointment));

        assertThatThrownBy(
                        () ->
                                service.validateAndArchiveAfterEventPromote(
                                        10L,
                                        5L,
                                        EventType.SERVICE,
                                        start.plus(Duration.ofMinutes(5)),
                                        end))
                .isInstanceOf(AppointmentValidationException.class)
                .hasMessageContaining("must match");
    }

    @Test
    void updateEntireSeriesPropagatesMetadataAndClearsDeadlineForNoneMode() {
        Instant anchorStart = LocalDateTime.of(2026, 7, 1, 18, 0).toInstant(ZoneOffset.UTC);
        Instant anchorEnd = anchorStart.plus(Duration.ofHours(2));
        AppointmentSeries series =
                AppointmentSeries.builder()
                        .id(3L)
                        .name("Old name")
                        .type(AppointmentType.SERVICE)
                        .location("Old hall")
                        .description("Old description")
                        .responseMode(AppointmentResponseMode.YES_NO)
                        .responseDeadlineMinutesBefore(30)
                        .audienceScope(AudienceScope.SELECTED_GROUPS)
                        .seriesStart(anchorStart)
                        .recurrenceRule("FREQ=WEEKLY")
                        .build();
        series.setAudienceGroupIds(new java.util.ArrayList<>(List.of(20L)));
        Appointment occurrence = seriesOccurrence(5L, series, anchorStart, anchorEnd, false);
        UpdateAppointmentRequest request = new UpdateAppointmentRequest();
        request.setName("New name");
        request.setType(AppointmentType.SERVICE);
        request.setLocation("New hall");
        request.setDescription("New description");
        request.setResponseMode(AppointmentResponseMode.NONE);
        request.setResponseDeadlineMinutesBefore(45);
        request.setAudienceScope(AudienceScope.ALL);
        request.setAudienceGroupIds(List.of(99L));
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.of(occurrence));
        when(appointmentRepository.findBySeries_Id(3L)).thenReturn(List.of(occurrence));
        when(mapper.toDto(occurrence)).thenReturn(AppointmentDto.builder().id(5L).build());

        service.updateAppointment(10L, 5L, request, SeriesEditScope.ENTIRE_SERIES);

        assertThat(series.getName()).isEqualTo("New name");
        assertThat(series.getType()).isEqualTo(AppointmentType.SERVICE);
        assertThat(series.getLocation()).isEqualTo("New hall");
        assertThat(series.getDescription()).isEqualTo("New description");
        assertThat(series.getResponseMode()).isEqualTo(AppointmentResponseMode.NONE);
        assertThat(series.getResponseDeadlineMinutesBefore()).isZero();
        assertThat(series.getAudienceScope()).isEqualTo(AudienceScope.ALL);
        assertThat(series.getAudienceGroupIds()).containsExactly(99L);
        assertThat(occurrence.getName()).isEqualTo("New name");
        assertThat(occurrence.getType()).isEqualTo(AppointmentType.SERVICE);
        assertThat(occurrence.getResponseDeadlineMinutesBefore()).isZero();
        assertThat(occurrence.getAudienceGroupIds()).containsExactly(99L);
    }

    @Test
    void updateEntireSeriesShiftsEndOnlyWithoutChangingStart() {
        Instant anchorStart = LocalDateTime.of(2026, 7, 1, 18, 0).toInstant(ZoneOffset.UTC);
        Instant anchorEnd = anchorStart.plus(Duration.ofHours(2));
        AppointmentSeries series =
                AppointmentSeries.builder()
                        .id(3L)
                        .seriesStart(anchorStart)
                        .recurrenceRule("FREQ=WEEKLY")
                        .build();
        Appointment occurrence = seriesOccurrence(5L, series, anchorStart, anchorEnd, false);
        UpdateAppointmentRequest request = new UpdateAppointmentRequest();
        request.setEnd(anchorEnd.plus(Duration.ofHours(1)));
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.of(occurrence));
        when(appointmentRepository.findBySeries_Id(3L)).thenReturn(List.of(occurrence));
        when(mapper.toDto(occurrence)).thenReturn(AppointmentDto.builder().id(5L).build());

        service.updateAppointment(10L, 5L, request, SeriesEditScope.ENTIRE_SERIES);

        assertThat(occurrence.getStart()).isEqualTo(anchorStart);
        assertThat(occurrence.getEnd()).isEqualTo(anchorEnd.plus(Duration.ofHours(1)));
        assertThat(series.getSeriesStart()).isEqualTo(anchorStart);
    }

    @Test
    void updateStandaloneAppointmentAppliesStartEndAndOccurrenceStart() {
        Instant oldStart = LocalDateTime.of(2026, 7, 1, 18, 0).toInstant(ZoneOffset.UTC);
        Instant oldEnd = oldStart.plus(Duration.ofHours(2));
        Instant newStart = oldStart.plus(Duration.ofDays(1));
        Instant newEnd = newStart.plus(Duration.ofHours(3));
        Appointment appointment =
                Appointment.builder()
                        .id(5L)
                        .series(null)
                        .archived(false)
                        .occurrenceStart(oldStart)
                        .start(oldStart)
                        .end(oldEnd)
                        .name("Training")
                        .type(AppointmentType.SERVICE)
                        .responseMode(AppointmentResponseMode.YES_NO)
                        .build();
        UpdateAppointmentRequest request = new UpdateAppointmentRequest();
        request.setStart(newStart);
        request.setEnd(newEnd);
        request.setLocation("Hall B");
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(appointment)).thenReturn(appointment);
        when(mapper.toDto(appointment)).thenReturn(AppointmentDto.builder().id(5L).build());

        service.updateAppointment(10L, 5L, request, null);

        assertThat(appointment.getStart()).isEqualTo(newStart);
        assertThat(appointment.getEnd()).isEqualTo(newEnd);
        assertThat(appointment.getOccurrenceStart()).isEqualTo(newStart);
        assertThat(appointment.getLocation()).isEqualTo("Hall B");
    }

    @Test
    void updateSeriesOccurrenceKeepsOccurrenceStartWhenStartChanges() {
        Instant occurrenceStart = LocalDateTime.of(2026, 7, 1, 18, 0).toInstant(ZoneOffset.UTC);
        Instant shiftedStart = occurrenceStart.plus(Duration.ofHours(1));
        AppointmentSeries series = AppointmentSeries.builder().id(3L).build();
        Appointment occurrence =
                seriesOccurrence(
                        5L,
                        series,
                        occurrenceStart,
                        occurrenceStart.plus(Duration.ofHours(2)),
                        false);
        UpdateAppointmentRequest request = new UpdateAppointmentRequest();
        request.setStart(shiftedStart);
        request.setEnd(shiftedStart.plus(Duration.ofHours(2)));
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.of(occurrence));
        when(appointmentRepository.save(occurrence)).thenReturn(occurrence);
        when(mapper.toDto(occurrence)).thenReturn(AppointmentDto.builder().id(5L).build());

        service.updateAppointment(10L, 5L, request, SeriesEditScope.THIS_OCCURRENCE);

        assertThat(occurrence.getOccurrenceStart()).isEqualTo(occurrenceStart);
        assertThat(occurrence.getStart()).isEqualTo(shiftedStart);
        assertThat(occurrence.getEnd()).isEqualTo(shiftedStart.plus(Duration.ofHours(2)));
        assertThat(occurrence.getIsException()).isTrue();
    }

    @Test
    void promoteArchivesMatchingPublicRelationsAndSpecialServiceAppointments() {
        Instant start = Instant.now().minus(Duration.ofHours(1));
        Instant end = start.plus(Duration.ofHours(2));
        Appointment publicRelations =
                Appointment.builder()
                        .id(5L)
                        .type(AppointmentType.SERVICE)
                        .start(start)
                        .end(end)
                        .archived(false)
                        .build();
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.of(publicRelations));
        when(appointmentRepository.save(publicRelations)).thenReturn(publicRelations);

        service.validateAndArchiveAfterEventPromote(10L, 5L, EventType.SERVICE, start, end);
        assertThat(publicRelations.isArchived()).isTrue();

        Appointment specialService =
                Appointment.builder()
                        .id(6L)
                        .type(AppointmentType.SERVICE)
                        .start(start)
                        .end(end)
                        .archived(false)
                        .build();
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(6L, 10L))
                .thenReturn(Optional.of(specialService));
        when(appointmentRepository.save(specialService)).thenReturn(specialService);

        service.validateAndArchiveAfterEventPromote(10L, 6L, EventType.SERVICE, start, end);
        assertThat(specialService.isArchived()).isTrue();
    }

    @Test
    void promoteRejectsAppointmentBeforeStartAndDoesNotArchive() {
        Appointment appointment =
                Appointment.builder()
                        .id(5L)
                        .type(AppointmentType.SERVICE)
                        .start(Instant.now().plus(Duration.ofHours(1)))
                        .end(Instant.now().plus(Duration.ofHours(2)))
                        .archived(false)
                        .build();
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_IdAndArchivedIsFalse(5L, 10L))
                .thenReturn(Optional.of(appointment));

        assertThatThrownBy(
                        () ->
                                service.validateAndArchiveAfterEventPromote(
                                        10L,
                                        5L,
                                        EventType.SERVICE,
                                        appointment.getStart(),
                                        appointment.getEnd()))
                .isInstanceOf(AppointmentValidationException.class)
                .hasMessageContaining("after the appointment has started");
        assertThat(appointment.isArchived()).isFalse();
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void countUnreadReturnsZeroWhenNoVisibleAppointments() {
        Instant now = Instant.now();
        Appointment upcoming =
                Appointment.builder()
                        .id(1L)
                        .responseMode(AppointmentResponseMode.YES_NO)
                        .start(now.plus(Duration.ofDays(1)))
                        .responseDeadlineMinutesBefore(0)
                        .archived(false)
                        .build();
        when(tokenService.requireUserId()).thenReturn(3L);
        when(membershipRepository.findActiveByUser_Id(eq(3L), any(Instant.class)))
                .thenReturn(
                        List.of(
                                UserOrganizationMembership.builder()
                                        .organization(Organization.builder().id(10L).build())
                                        .startDate(Instant.now())
                                        .build()));
        when(appointmentRepository.findUpcomingByOrganizationId(eq(10L), any()))
                .thenReturn(List.of(upcoming));
        when(audienceService.filterAppointmentsVisibleToUser(3L, List.of(upcoming)))
                .thenReturn(List.of());

        assertThat(service.countUnreadAcrossOrganizations()).isEqualTo(Map.of(10L, 0L));
        verify(readRepository, never()).findAppointmentIdsByUserIdAndAppointmentIdIn(any(), any());
    }

    @Test
    void countUnreadIgnoresAppointmentsBeyondListHorizon() {
        Instant now = Instant.now();
        Appointment visibleUnread =
                Appointment.builder()
                        .id(1L)
                        .responseMode(AppointmentResponseMode.NONE)
                        .start(now.atZone(ZoneOffset.UTC).plusMonths(1).toInstant())
                        .responseDeadlineMinutesBefore(0)
                        .archived(false)
                        .build();
        Appointment beyondHorizon =
                Appointment.builder()
                        .id(2L)
                        .responseMode(AppointmentResponseMode.NONE)
                        .start(
                                now.atZone(ZoneOffset.UTC)
                                        .plusMonths(
                                                AppointmentRecurrenceService.SERIES_HORIZON_MONTHS)
                                        .toInstant()
                                        .plus(Duration.ofDays(5)))
                        .responseDeadlineMinutesBefore(0)
                        .archived(false)
                        .build();
        when(tokenService.requireUserId()).thenReturn(3L);
        when(membershipRepository.findActiveByUser_Id(eq(3L), any(Instant.class)))
                .thenReturn(
                        List.of(
                                UserOrganizationMembership.builder()
                                        .organization(Organization.builder().id(10L).build())
                                        .startDate(Instant.now())
                                        .build()));
        when(appointmentRepository.findUpcomingByOrganizationId(eq(10L), any()))
                .thenReturn(List.of(visibleUnread, beyondHorizon));
        when(audienceService.filterAppointmentsVisibleToUser(
                        3L, List.of(visibleUnread, beyondHorizon)))
                .thenReturn(List.of(visibleUnread, beyondHorizon));
        when(readRepository.findAppointmentIdsByUserIdAndAppointmentIdIn(eq(3L), any()))
                .thenReturn(List.of());

        assertThat(service.countUnreadAcrossOrganizations()).isEqualTo(Map.of(10L, 1L));
        verify(readRepository)
                .findAppointmentIdsByUserIdAndAppointmentIdIn(eq(3L), eq(List.of(1L)));
    }

    @Test
    void countUnreadExcludesAlreadyReadAppointments() {
        Instant now = Instant.now();
        Appointment unread =
                Appointment.builder()
                        .id(1L)
                        .responseMode(AppointmentResponseMode.NONE)
                        .start(now.plus(Duration.ofDays(2)))
                        .responseDeadlineMinutesBefore(0)
                        .archived(false)
                        .build();
        Appointment alreadyRead =
                Appointment.builder()
                        .id(2L)
                        .responseMode(AppointmentResponseMode.YES_NO)
                        .start(now.plus(Duration.ofDays(1)))
                        .responseDeadlineMinutesBefore(0)
                        .archived(false)
                        .build();
        when(tokenService.requireUserId()).thenReturn(3L);
        when(membershipRepository.findActiveByUser_Id(eq(3L), any(Instant.class)))
                .thenReturn(
                        List.of(
                                UserOrganizationMembership.builder()
                                        .organization(Organization.builder().id(10L).build())
                                        .startDate(Instant.now())
                                        .build()));
        when(appointmentRepository.findUpcomingByOrganizationId(eq(10L), any()))
                .thenReturn(List.of(unread, alreadyRead));
        when(audienceService.filterAppointmentsVisibleToUser(3L, List.of(unread, alreadyRead)))
                .thenReturn(List.of(unread, alreadyRead));
        when(readRepository.findAppointmentIdsByUserIdAndAppointmentIdIn(eq(3L), any()))
                .thenReturn(List.of(2L));

        assertThat(service.countUnreadAcrossOrganizations()).isEqualTo(Map.of(10L, 1L));
        verify(readRepository)
                .findAppointmentIdsByUserIdAndAppointmentIdIn(eq(3L), eq(List.of(1L, 2L)));
    }

    @Test
    void markAllReadSavesOnlyUnreadVisibleAppointments() {
        Instant now = Instant.now();
        Appointment unread =
                Appointment.builder()
                        .id(1L)
                        .responseMode(AppointmentResponseMode.NONE)
                        .start(now.plus(Duration.ofDays(2)))
                        .archived(false)
                        .build();
        Appointment alreadyRead =
                Appointment.builder()
                        .id(2L)
                        .responseMode(AppointmentResponseMode.NONE)
                        .start(now.plus(Duration.ofDays(1)))
                        .archived(false)
                        .build();
        when(tokenService.requireTenantId()).thenReturn(7L);
        when(tokenService.requireUserId()).thenReturn(3L);
        when(appointmentRepository.findByOrganizationIdAndDateRange(eq(10L), any(), any()))
                .thenReturn(List.of(unread, alreadyRead));
        when(audienceService.filterAppointmentsVisibleToUser(3L, List.of(unread, alreadyRead)))
                .thenReturn(List.of(unread, alreadyRead));
        when(readRepository.findAppointmentIdsByUserIdAndAppointmentIdIn(
                        eq(3L), eq(List.of(1L, 2L))))
                .thenReturn(List.of(2L));

        service.markAllReadForOrganization(10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AppointmentRead>> captor = ArgumentCaptor.forClass(List.class);
        verify(readRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().getFirst().getAppointment().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getFirst().getUserId()).isEqualTo(3L);
    }

    private Appointment seriesOccurrence(
            Long id, AppointmentSeries series, Instant start, Instant end, boolean isException) {
        return Appointment.builder()
                .id(id)
                .series(series)
                .occurrenceStart(start)
                .start(start)
                .end(end)
                .name("Training")
                .type(AppointmentType.SERVICE)
                .responseMode(AppointmentResponseMode.YES_NO)
                .audienceScope(AudienceScope.SELECTED_GROUPS)
                .isException(isException)
                .archived(false)
                .build();
    }

    private CreateAppointmentRequest request() {
        CreateAppointmentRequest request = new CreateAppointmentRequest();
        request.setOrganizationId(10L);
        request.setName("Training");
        request.setType(AppointmentType.SERVICE);
        request.setStart(LocalDateTime.of(2026, 7, 1, 18, 0).toInstant(ZoneOffset.UTC));
        request.setEnd(LocalDateTime.of(2026, 7, 1, 20, 0).toInstant(ZoneOffset.UTC));
        request.setResponseMode(AppointmentResponseMode.YES_NO);
        request.setAudienceScope(AudienceScope.SELECTED_GROUPS);
        request.setAudienceGroupIds(List.of(20L));
        return request;
    }
}
