/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CourseOfferingCancellationServiceTest {

    private static final long TENANT_ID = 7L;
    private static final long ORG_ID = 10L;
    private static final long OFFERING_ID = 100L;
    private static final long ACTOR_ID = 8L;

    @Mock CourseOfferingRepository offeringRepository;
    @Mock CourseRegistrationRepository registrationRepository;
    @Mock CourseOfferingService offeringService;
    @Mock WorkflowService workflowService;
    @Mock TokenService tokenService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock TenantTrainingCourseTypeRepository trainingCourseTypeRepository;

    @InjectMocks CourseOfferingCancellationService service;

    private CourseOffering offering;

    @BeforeEach
    void setUp() {
        offering = offering(CourseOfferingStatus.PUBLISHED);
        lenient().when(tokenService.requireTenantId()).thenReturn(TENANT_ID);
        lenient().when(tokenService.getReferencedUserId()).thenReturn(ACTOR_ID);
        lenient()
                .when(offeringRepository.findForUpdate(OFFERING_ID))
                .thenReturn(Optional.of(offering));
        lenient()
                .when(offeringService.get(ORG_ID, OFFERING_ID))
                .thenReturn(CourseOfferingDto.builder().id(OFFERING_ID).build());
        TenantTrainingCourseType type = new TenantTrainingCourseType();
        type.setId(5L);
        type.setTenantId(TENANT_ID);
        type.setName("Atemschutz");
        lenient()
                .when(trainingCourseTypeRepository.findById(5L))
                .thenReturn(java.util.Optional.of(type));
    }

    @Test
    void cancelEndsAllLiveRegistrationsAsEndedByTheOrganization() {
        CourseRegistration pending = registration(1L, 31L, CourseRegistrationStatus.PENDING);
        CourseRegistration participant =
                registration(2L, 32L, CourseRegistrationStatus.PARTICIPANT);
        CourseRegistration waitlist = registration(3L, 33L, CourseRegistrationStatus.WAITLIST);
        stubLive(pending, participant, waitlist);

        service.cancel(ORG_ID, OFFERING_ID, "  Zu wenige Anmeldungen ");

        for (CourseRegistration registration : List.of(pending, participant, waitlist)) {
            assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.CANCELLED);
            assertThat(registration.getEndedBy()).isEqualTo(CourseRegistrationEndedBy.ORGANIZATION);
            assertThat(registration.getEndedByUserId()).isEqualTo(ACTOR_ID);
            assertThat(registration.getEndReason()).isEqualTo("Zu wenige Anmeldungen");
            assertThat(registration.getEndedAt()).isNotNull();
            verify(registrationRepository).save(registration);
        }
        assertThat(offering.getStatus()).isEqualTo(CourseOfferingStatus.CANCELLED);
        assertThat(offering.getCancelledAt()).isNotNull();
        assertThat(offering.getCancelReason()).isEqualTo("Zu wenige Anmeldungen");
        verify(offeringRepository).saveAndFlush(offering);
    }

    @Test
    void cancelClosesRegistrationAndCancellationWorkflows() {
        CourseRegistration pending = registration(1L, 31L, CourseRegistrationStatus.PENDING);
        pending.setWorkflowId(900L);
        CourseRegistration participant =
                registration(2L, 32L, CourseRegistrationStatus.PARTICIPANT);
        participant.setWorkflowId(901L); // already approved: not closed again
        participant.setCancellationWorkflowId(902L);
        stubLive(pending, participant);

        service.cancel(ORG_ID, OFFERING_ID, "Absage");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<Long>> captor =
                ArgumentCaptor.forClass(java.util.Collection.class);
        verify(workflowService).cancelActiveWorkflows(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(900L, 902L);
    }

    @Test
    void cancelPublishesOneEventForAllAffectedUsers() {
        stubLive(
                registration(1L, 31L, CourseRegistrationStatus.PENDING),
                registration(2L, 32L, CourseRegistrationStatus.PARTICIPANT),
                registration(3L, 33L, CourseRegistrationStatus.WAITLIST));

        service.cancel(ORG_ID, OFFERING_ID, "Absage");

        ArgumentCaptor<CourseOfferingCancelledEvent> captor =
                ArgumentCaptor.forClass(CourseOfferingCancelledEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        CourseOfferingCancelledEvent event = captor.getValue();
        assertThat(event.tenantId()).isEqualTo(TENANT_ID);
        assertThat(event.offeringId()).isEqualTo(OFFERING_ID);
        assertThat(event.trainingCourseTypeName()).isEqualTo("Atemschutz");
        assertThat(event.reason()).isEqualTo("Absage");
        assertThat(event.userIds()).containsExactlyInAnyOrder(31L, 32L, 33L);
    }

    @Test
    void cancelWithoutRegistrationsStillCancelsAndNotifiesNobody() {
        stubLive();

        service.cancel(ORG_ID, OFFERING_ID, "Absage");

        assertThat(offering.getStatus()).isEqualTo(CourseOfferingStatus.CANCELLED);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void cancelRequiresAReason() {
        assertThatThrownBy(() -> service.cancel(ORG_ID, OFFERING_ID, "  "))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("reason");
        assertThatThrownBy(() -> service.cancel(ORG_ID, OFFERING_ID, null))
                .isInstanceOf(CourseOfferingValidationException.class);
        verify(offeringRepository, never()).findForUpdate(any());
    }

    @Test
    void completedOfferingCannotBeCancelled() {
        offering.setStatus(CourseOfferingStatus.COMPLETED);

        assertThatThrownBy(() -> service.cancel(ORG_ID, OFFERING_ID, "Absage"))
                .isInstanceOf(CourseOfferingValidationException.class);
        verifyNothingChanged();
    }

    @Test
    void cancelledOfferingCannotBeCancelledTwice() {
        offering.setStatus(CourseOfferingStatus.CANCELLED);

        assertThatThrownBy(() -> service.cancel(ORG_ID, OFFERING_ID, "Absage"))
                .isInstanceOf(CourseOfferingValidationException.class);
        verifyNothingChanged();
    }

    @Test
    void draftsAndTemplatesCannotBeCancelled() {
        offering.setStatus(CourseOfferingStatus.DRAFT);
        assertThatThrownBy(() -> service.cancel(ORG_ID, OFFERING_ID, "Absage"))
                .isInstanceOf(CourseOfferingValidationException.class);

        offering.setStatus(CourseOfferingStatus.TEMPLATE);
        assertThatThrownBy(() -> service.cancel(ORG_ID, OFFERING_ID, "Absage"))
                .isInstanceOf(CourseOfferingValidationException.class);
        verifyNothingChanged();
    }

    @Test
    void offeringOfAnotherOrganizationIsNotFound() {
        assertThatThrownBy(() -> service.cancel(99L, OFFERING_ID, "Absage"))
                .isInstanceOf(CourseOfferingNotFoundException.class);
        verifyNothingChanged();
    }

    private void verifyNothingChanged() {
        verify(registrationRepository, never()).save(any());
        verify(workflowService, never()).cancelActiveWorkflows(any());
        verify(eventPublisher, never()).publishEvent(any());
        verify(offeringRepository, never()).saveAndFlush(any());
    }

    private void stubLive(CourseRegistration... registrations) {
        when(registrationRepository.findByOffering_IdAndStatusIn(eq(OFFERING_ID), any()))
                .thenReturn(List.of(registrations));
    }

    private CourseRegistration registration(long id, long userId, CourseRegistrationStatus status) {
        return CourseRegistration.builder()
                .id(id)
                .tenantId(TENANT_ID)
                .offering(offering)
                .userId(userId)
                .status(status)
                .registeredAt(Instant.parse("2027-01-01T00:00:00Z"))
                .build();
    }

    private static CourseOffering offering(CourseOfferingStatus status) {
        return CourseOffering.builder()
                .id(OFFERING_ID)
                .tenantId(TENANT_ID)
                .organizationId(ORG_ID)
                .trainingCourseTypeId(5L)
                .status(status)
                .maxParticipants(10)
                .sessions(new ArrayList<>())
                .audiences(new ArrayList<>())
                .prerequisites(new ArrayList<>())
                .build();
    }
}
