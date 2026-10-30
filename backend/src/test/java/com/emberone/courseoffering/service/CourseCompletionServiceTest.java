/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.domain.CourseOfferingStatus;
import com.emberone.courseoffering.domain.CourseRegistration;
import com.emberone.courseoffering.domain.CourseRegistrationEndedBy;
import com.emberone.courseoffering.domain.CourseRegistrationStatus;
import com.emberone.courseoffering.domain.CourseSession;
import com.emberone.courseoffering.domain.CourseSessionType;
import com.emberone.courseoffering.dto.CourseCompletionDto;
import com.emberone.courseoffering.dto.CourseRegistrationDto;
import com.emberone.courseoffering.exception.CourseOfferingValidationException;
import com.emberone.courseoffering.exception.CourseRegistrationNotFoundException;
import com.emberone.courseoffering.repository.CourseOfferingRepository;
import com.emberone.courseoffering.repository.CourseRegistrationRepository;
import com.emberone.platform.security.TokenService;
import com.emberone.trainingcourse.service.TrainingCourseRecordService;
import com.emberone.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseCompletionServiceTest {

    private static final long TENANT_ID = 7L;
    private static final long ORG_ID = 10L;
    private static final long OFFERING_ID = 100L;
    private static final long TYPE_ID = 55L;
    private static final long ACTOR_ID = 999L;
    private static final Instant BASE = Instant.parse("2025-03-01T08:00:00Z");

    @Mock CourseOfferingRepository offeringRepository;
    @Mock CourseRegistrationRepository registrationRepository;
    @Mock CourseRegistrationAssembler assembler;
    @Mock TrainingCourseRecordService trainingCourseRecordService;
    @Mock WorkflowService workflowService;
    @Mock TokenService tokenService;

    CourseCompletionService service;

    private CourseOffering offering;
    private CourseRegistration passed;
    private CourseRegistration failed;

    @BeforeEach
    void setUp() {
        service =
                new CourseCompletionService(
                        offeringRepository,
                        registrationRepository,
                        assembler,
                        trainingCourseRecordService,
                        workflowService,
                        tokenService);
        offering =
                CourseOffering.builder()
                        .id(OFFERING_ID)
                        .tenantId(TENANT_ID)
                        .organizationId(ORG_ID)
                        .trainingCourseTypeId(TYPE_ID)
                        .status(CourseOfferingStatus.PUBLISHED)
                        .build();
        offering.getSessions().add(session(1, 60, CourseSessionType.THEORY));
        offering.getSessions().add(session(2, 180, CourseSessionType.THEORY));
        offering.syncSchedule();
        passed = registration(40L);
        failed = registration(41L);

        lenient().when(tokenService.requireTenantId()).thenReturn(TENANT_ID);
        lenient().when(tokenService.getReferencedUserId()).thenReturn(ACTOR_ID);
        lenient()
                .when(offeringRepository.findForUpdate(OFFERING_ID))
                .thenReturn(Optional.of(offering));
        lenient()
                .when(offeringRepository.findByIdAndOrganizationId(OFFERING_ID, ORG_ID))
                .thenReturn(Optional.of(offering));
        lenient()
                .when(
                        registrationRepository.findByOffering_IdAndStatusOrderByApprovedAtAscIdAsc(
                                OFFERING_ID, CourseRegistrationStatus.PARTICIPANT))
                .thenReturn(List.of(passed, failed));
        lenient()
                .when(registrationRepository.findByIdAndOffering_Id(anyLong(), eq(OFFERING_ID)))
                .thenAnswer(
                        call ->
                                List.of(passed, failed).stream()
                                        .filter(r -> r.getId().equals(call.getArgument(0)))
                                        .findFirst());
        lenient()
                .when(assembler.toDtos(eq(OFFERING_ID), any()))
                .thenReturn(
                        List.of(
                                CourseRegistrationDto.builder()
                                        .id(40L)
                                        .userDisplayName("A")
                                        .build(),
                                CourseRegistrationDto.builder()
                                        .id(41L)
                                        .userDisplayName("B")
                                        .build()));
    }

    @Test
    void sessionHoursSumsSessionMinutes() {
        assertThat(CourseCompletionService.sessionHours(offering))
                .isEqualByComparingTo(new BigDecimal("4.00"));
    }

    @Test
    void completeRejectsBeforeFirstSessionDay() {
        shiftSessions(
                Instant.now().plus(30, ChronoUnit.DAYS), Instant.now().plus(31, ChronoUnit.DAYS));

        assertThatThrownBy(() -> service.complete(ORG_ID, OFFERING_ID))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("has not started yet");
        verify(trainingCourseRecordService, never())
                .createSubmissionFromCourseRegistration(
                        anyLong(), anyLong(), anyLong(), any(), any(), any(), any(), any(),
                        anyLong());
    }

    @Test
    void setFinalResultStoresDecision() {
        CourseCompletionDto dto = service.setFinalResult(ORG_ID, OFFERING_ID, 40L, true);

        assertThat(passed.getFinalPassed()).isTrue();
        assertThat(passed.getFinalDecidedByUserId()).isEqualTo(ACTOR_ID);
        assertThat(row(dto, 40L).getFinalPassed()).isTrue();
        verify(registrationRepository).save(passed);
    }

    @Test
    void setFinalResultRejectsBeforeFirstSessionDay() {
        shiftSessions(
                Instant.now().plus(30, ChronoUnit.DAYS), Instant.now().plus(31, ChronoUnit.DAYS));

        assertThatThrownBy(() -> service.setFinalResult(ORG_ID, OFFERING_ID, 40L, true))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("has not started yet");
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void completeCreditsOnlyMarkedPassed() {
        passed.setFinalPassed(true);
        failed.setFinalPassed(false);

        service.complete(ORG_ID, OFFERING_ID);

        verify(trainingCourseRecordService)
                .createSubmissionFromCourseRegistration(
                        eq(TENANT_ID),
                        eq(passed.getUserId()),
                        eq(TYPE_ID),
                        eq(java.time.LocalDate.of(2025, 3, 2)),
                        eq(java.time.LocalDate.of(2025, 3, 3)),
                        any(),
                        isNull(),
                        eq(new BigDecimal("4.00")),
                        eq(passed.getId()));
        verify(trainingCourseRecordService, org.mockito.Mockito.times(1))
                .createSubmissionFromCourseRegistration(
                        anyLong(), anyLong(), anyLong(), any(), any(), any(), any(), any(),
                        anyLong());
        assertThat(offering.getStatus()).isEqualTo(CourseOfferingStatus.COMPLETED);
    }

    @Test
    void completeWithoutDecisionsCreditsNobody() {
        service.complete(ORG_ID, OFFERING_ID);

        verify(trainingCourseRecordService, never())
                .createSubmissionFromCourseRegistration(
                        anyLong(), anyLong(), anyLong(), any(), any(), any(), any(), any(),
                        anyLong());
    }

    @Test
    void completeEndsOpenApplicationsAndClosesWorkflows() {
        passed.setFinalPassed(true);
        CourseRegistration pending = registration(43L);
        pending.setStatus(CourseRegistrationStatus.PENDING);
        pending.setWorkflowId(900L);
        when(registrationRepository.findByOffering_IdAndStatusIn(eq(OFFERING_ID), any()))
                .thenReturn(List.of(pending));

        service.complete(ORG_ID, OFFERING_ID);

        assertThat(pending.getStatus()).isEqualTo(CourseRegistrationStatus.CANCELLED);
        assertThat(pending.getEndedBy()).isEqualTo(CourseRegistrationEndedBy.ORGANIZATION);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<Long>> captor =
                ArgumentCaptor.forClass(java.util.Collection.class);
        verify(workflowService).cancelActiveWorkflows(captor.capture());
        assertThat(captor.getValue()).containsExactly(900L);
    }

    @Test
    void setFinalResultRejectsNonParticipant() {
        CourseRegistration waiting = registration(43L);
        waiting.setStatus(CourseRegistrationStatus.WAITLIST);
        when(registrationRepository.findByIdAndOffering_Id(43L, OFFERING_ID))
                .thenReturn(Optional.of(waiting));

        assertThatThrownBy(() -> service.setFinalResult(ORG_ID, OFFERING_ID, 43L, true))
                .isInstanceOf(CourseOfferingValidationException.class);
        assertThatThrownBy(() -> service.setFinalResult(ORG_ID, OFFERING_ID, 99L, true))
                .isInstanceOf(CourseRegistrationNotFoundException.class);
        verify(registrationRepository, never()).save(any());
    }

    private static CourseCompletionDto.Participant row(CourseCompletionDto dto, long id) {
        return dto.getParticipants().stream()
                .filter(p -> p.getRegistrationId() == id)
                .findFirst()
                .orElseThrow();
    }

    private static CourseRegistration registration(long id) {
        return CourseRegistration.builder()
                .id(id)
                .tenantId(TENANT_ID)
                .userId(id + 1000)
                .status(CourseRegistrationStatus.PARTICIPANT)
                .build();
    }

    private void shiftSessions(Instant start, Instant end) {
        offering.getSessions().clear();
        offering.getSessions()
                .add(
                        CourseSession.builder()
                                .id(1L)
                                .type(CourseSessionType.THEORY)
                                .start(start)
                                .end(end)
                                .location("Halle")
                                .build());
        offering.syncSchedule();
    }

    private static CourseSession session(long id, int minutes, CourseSessionType type) {
        Instant start = BASE.plus(id, ChronoUnit.DAYS);
        return CourseSession.builder()
                .id(id)
                .type(type)
                .start(start)
                .end(start.plus(minutes, ChronoUnit.MINUTES))
                .location("Halle")
                .build();
    }
}
