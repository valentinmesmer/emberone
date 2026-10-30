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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.courseoffering.domain.CourseAudienceKind;
import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.domain.CourseOfferingAudience;
import com.emberone.courseoffering.domain.CourseOfferingStatus;
import com.emberone.courseoffering.domain.CourseRegistration;
import com.emberone.courseoffering.domain.CourseRegistrationEndedBy;
import com.emberone.courseoffering.domain.CourseRegistrationStatus;
import com.emberone.courseoffering.dto.CoursePrerequisiteCheckDto;
import com.emberone.courseoffering.dto.CourseRegistrationDto;
import com.emberone.courseoffering.event.CourseRegistrationRemovedEvent;
import com.emberone.courseoffering.exception.CourseOfferingNotFoundException;
import com.emberone.courseoffering.exception.CourseOfferingValidationException;
import com.emberone.courseoffering.exception.CourseRegistrationNotFoundException;
import com.emberone.courseoffering.repository.CourseOfferingRepository;
import com.emberone.courseoffering.repository.CourseRegistrationRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.security.TokenService;
import com.emberone.trainingcourse.repository.TrainingCourseUserSubmissionRepository;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    private static final long TENANT_ID = 7L;
    private static final long ORG_ID = 10L;
    private static final long OFFERING_ID = 100L;
    private static final long USER_ID = 3L;
    private static final long TYPE_ID = 55L;
    private static final long WORKFLOW_ID = 900L;
    private static final long REGISTRATION_ID = 40L;

    @Mock CourseOfferingRepository offeringRepository;
    @Mock CourseRegistrationRepository registrationRepository;
    @Mock CourseAudienceScopeService scopeService;
    @Mock CoursePrerequisiteService prerequisiteService;
    @Mock TrainingCourseUserSubmissionRepository submissionRepository;
    @Mock CourseRegistrationAssembler assembler;
    @Mock TokenService tokenService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock EntityManager entityManager;
    @Mock TenantTrainingCourseTypeRepository trainingCourseTypeRepository;

    @InjectMocks CourseRegistrationService service;

    private CourseOffering offering;

    @BeforeEach
    void setUp() {
        offering = publishedOffering(2);
        lenient().when(tokenService.requireTenantId()).thenReturn(TENANT_ID);
        lenient().when(tokenService.getReferencedUserId()).thenReturn(8L);
        lenient()
                .when(offeringRepository.findForUpdate(OFFERING_ID))
                .thenReturn(Optional.of(offering));
        lenient()
                .when(registrationRepository.save(any(CourseRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient()
                .when(assembler.toDtos(eq(OFFERING_ID), any()))
                .thenReturn(List.of(CourseRegistrationDto.builder().id(REGISTRATION_ID).build()));
        TenantTrainingCourseType type = new TenantTrainingCourseType();
        type.setId(TYPE_ID);
        type.setTenantId(TENANT_ID);
        type.setName("Atemschutz");
        lenient()
                .when(trainingCourseTypeRepository.findById(TYPE_ID))
                .thenReturn(java.util.Optional.of(type));
    }

    // --- requireRegistrable ---

    @Test
    void requireRegistrableRejectsOfferingThatIsNotVisibleToTheUser() {
        when(scopeService.forUser(USER_ID))
                .thenReturn(
                        new CourseAudienceScope(
                                java.util.Set.of(), java.util.Set.of(), java.util.Set.of()));
        when(offeringRepository.findOne(anySpec())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireRegistrable(USER_ID, OFFERING_ID))
                .isInstanceOf(CourseOfferingNotFoundException.class);
    }

    @Test
    void requireRegistrableRejectsClosedRegistrationWindow() {
        offering.setRegistrationEnd(Instant.now().minus(1, ChronoUnit.HOURS));
        eligible();

        assertThatThrownBy(() -> service.requireRegistrable(USER_ID, OFFERING_ID))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("not open");
    }

    @Test
    void requireRegistrableRejectsRegistrationWindowThatHasNotStarted() {
        offering.setRegistrationStart(Instant.now().plus(1, ChronoUnit.HOURS));
        offering.setRegistrationEnd(Instant.now().plus(2, ChronoUnit.HOURS));
        eligible();

        assertThatThrownBy(() -> service.requireRegistrable(USER_ID, OFFERING_ID))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("not open");
    }

    @Test
    void requireRegistrableRejectsActiveRegistration() {
        eligible();
        when(registrationRepository.existsByOffering_IdAndUserIdAndStatusIn(
                        eq(OFFERING_ID), eq(USER_ID), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.requireRegistrable(USER_ID, OFFERING_ID))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("already have an active registration");
    }

    @Test
    void requireRegistrableRejectsUnmetPrerequisites() {
        eligible();
        when(prerequisiteService.check(offering, USER_ID))
                .thenReturn(CoursePrerequisiteCheckDto.builder().met(false).build());

        assertThatThrownBy(() -> service.requireRegistrable(USER_ID, OFFERING_ID))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("prerequisites");
    }

    @Test
    void requireRegistrableReturnsOfferingAndHeldFlag() {
        eligible();
        when(prerequisiteService.check(offering, USER_ID))
                .thenReturn(CoursePrerequisiteCheckDto.builder().met(true).build());
        when(submissionRepository.findGrantedTypeIdsByUser(USER_ID)).thenReturn(List.of(TYPE_ID));

        CourseRegistrationService.Candidate candidate =
                service.requireRegistrable(USER_ID, OFFERING_ID);

        assertThat(candidate.offering()).isSameAs(offering);
        assertThat(candidate.alreadyHeld()).isTrue();
    }

    @Test
    void requireRegistrableMarksCourseNotHeldWhenTypeIsMissing() {
        eligible();
        when(prerequisiteService.check(offering, USER_ID))
                .thenReturn(CoursePrerequisiteCheckDto.builder().met(true).build());
        when(submissionRepository.findGrantedTypeIdsByUser(USER_ID))
                .thenReturn(List.of(TYPE_ID + 1));

        assertThat(service.requireRegistrable(USER_ID, OFFERING_ID).alreadyHeld()).isFalse();
    }

    // --- createPending / approve ---

    @Test
    void createPendingStoresPendingRegistrationForTheWorkflow() {
        when(offeringRepository.getReferenceById(OFFERING_ID)).thenReturn(offering);

        CourseRegistration saved =
                service.createPending(TENANT_ID, USER_ID, OFFERING_ID, WORKFLOW_ID, true);

        assertThat(saved.getStatus()).isEqualTo(CourseRegistrationStatus.PENDING);
        assertThat(saved.getWorkflowId()).isEqualTo(WORKFLOW_ID);
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.isAlreadyHeld()).isTrue();
        assertThat(saved.getRegisteredAt()).isNotNull();
        assertThat(saved.getApprovedAt()).isNull();
    }

    @Test
    void approveGivesFreeSeatToParticipant() {
        CourseRegistration registration = pending();
        when(registrationRepository.findByWorkflowId(WORKFLOW_ID))
                .thenReturn(Optional.of(registration));
        participants(1);

        service.approve(WORKFLOW_ID);

        assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.PARTICIPANT);
        assertThat(registration.getApprovedAt()).isNotNull();
    }

    @Test
    void approvePutsRegistrationOnWaitlistWhenFull() {
        CourseRegistration registration = pending();
        when(registrationRepository.findByWorkflowId(WORKFLOW_ID))
                .thenReturn(Optional.of(registration));
        participants(2);

        service.approve(WORKFLOW_ID);

        assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.WAITLIST);
        assertThat(registration.getApprovedAt()).isNotNull();
    }

    @Test
    void approveLocksOfferingBeforeCountingSeats() {
        CourseRegistration registration = pending();
        when(registrationRepository.findByWorkflowId(WORKFLOW_ID))
                .thenReturn(Optional.of(registration));
        participants(0);

        service.approve(WORKFLOW_ID);

        InOrder order = inOrder(offeringRepository, registrationRepository);
        order.verify(offeringRepository).findForUpdate(OFFERING_ID);
        order.verify(registrationRepository)
                .countByOffering_IdAndStatus(OFFERING_ID, CourseRegistrationStatus.PARTICIPANT);
    }

    @Test
    void approveRejectsOfferingThatIsNoLongerPublished() {
        offering.setStatus(CourseOfferingStatus.CANCELLED);
        when(registrationRepository.findByWorkflowId(WORKFLOW_ID))
                .thenReturn(Optional.of(pending()));

        assertThatThrownBy(() -> service.approve(WORKFLOW_ID))
                .isInstanceOf(CourseOfferingValidationException.class);
    }

    @Test
    void approveRejectsRegistrationThatIsNotPending() {
        CourseRegistration registration = pending();
        registration.setStatus(CourseRegistrationStatus.PARTICIPANT);
        when(registrationRepository.findByWorkflowId(WORKFLOW_ID))
                .thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> service.approve(WORKFLOW_ID))
                .isInstanceOf(CourseOfferingValidationException.class);
    }

    @Test
    void approveFailsWithoutRegistration() {
        when(registrationRepository.findByWorkflowId(WORKFLOW_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(WORKFLOW_ID))
                .isInstanceOf(CourseOfferingValidationException.class);
    }

    // --- reject / cancel ---

    @Test
    void rejectEndsPendingRegistration() {
        CourseRegistration registration = pending();
        when(registrationRepository.findByWorkflowId(WORKFLOW_ID))
                .thenReturn(Optional.of(registration));

        service.reject(WORKFLOW_ID);

        assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.REJECTED);
        assertThat(registration.getEndedAt()).isNotNull();
        assertThat(registration.getEndedBy()).isNull();
    }

    @Test
    void cancelByUserEndsPendingRegistrationAsUserCancelled() {
        CourseRegistration registration = pending();
        when(registrationRepository.findByWorkflowId(WORKFLOW_ID))
                .thenReturn(Optional.of(registration));

        service.cancelByUser(WORKFLOW_ID);

        assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.CANCELLED);
        assertThat(registration.getEndedBy()).isEqualTo(CourseRegistrationEndedBy.USER);
        assertThat(registration.getEndedAt()).isNotNull();
    }

    @Test
    void cancelByUserLeavesAlreadyDecidedRegistrationAlone() {
        CourseRegistration registration = pending();
        registration.setStatus(CourseRegistrationStatus.PARTICIPANT);
        when(registrationRepository.findByWorkflowId(WORKFLOW_ID))
                .thenReturn(Optional.of(registration));

        service.cancelByUser(WORKFLOW_ID);

        assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.PARTICIPANT);
        verify(registrationRepository, never()).save(any());
    }

    // --- promote ---

    @Test
    void promoteMovesWaitlistRegistrationIntoFreeSeat() {
        CourseRegistration registration = waitlisted();
        found(registration);
        participants(1);

        service.promote(ORG_ID, OFFERING_ID, REGISTRATION_ID);

        assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.PARTICIPANT);
        assertThat(registration.getApprovedAt()).isEqualTo(Instant.parse("2027-01-01T10:00:00Z"));
    }

    @Test
    void promoteRespectsTheOverallMaximum() {
        CourseRegistration registration = waitlisted();
        found(registration);
        participants(2);

        assertThatThrownBy(() -> service.promote(ORG_ID, OFFERING_ID, REGISTRATION_ID))
                .isInstanceOf(ConflictException.class);

        assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.WAITLIST);
    }

    @Test
    void promoteOnlyAcceptsWaitlistRegistrations() {
        CourseRegistration registration = waitlisted();
        registration.setStatus(CourseRegistrationStatus.PARTICIPANT);
        found(registration);

        assertThatThrownBy(() -> service.promote(ORG_ID, OFFERING_ID, REGISTRATION_ID))
                .isInstanceOf(CourseOfferingValidationException.class);
    }

    @Test
    void promoteHidesOfferingsOfOtherOrganizations() {
        assertThatThrownBy(() -> service.promote(ORG_ID + 1, OFFERING_ID, REGISTRATION_ID))
                .isInstanceOf(CourseOfferingNotFoundException.class);
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void promoteFailsForUnknownRegistration() {
        when(registrationRepository.findByIdAndOffering_Id(REGISTRATION_ID, OFFERING_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.promote(ORG_ID, OFFERING_ID, REGISTRATION_ID))
                .isInstanceOf(CourseRegistrationNotFoundException.class);
    }

    @Test
    void promoteRequiresPublishedOffering() {
        offering.setStatus(CourseOfferingStatus.COMPLETED);

        assertThatThrownBy(() -> service.promote(ORG_ID, OFFERING_ID, REGISTRATION_ID))
                .isInstanceOf(CourseOfferingValidationException.class);
    }

    // --- remove ---

    @Test
    void removeParticipantRecordsOrganizationAsCanceller() {
        CourseRegistration registration = waitlisted();
        registration.setStatus(CourseRegistrationStatus.PARTICIPANT);
        found(registration);

        service.remove(ORG_ID, OFFERING_ID, REGISTRATION_ID, "  Kein Bedarf mehr  ");

        assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.CANCELLED);
        assertThat(registration.getEndedBy()).isEqualTo(CourseRegistrationEndedBy.ORGANIZATION);
        assertThat(registration.getEndedByUserId()).isEqualTo(8L);
        assertThat(registration.getEndedAt()).isNotNull();
        assertThat(registration.getEndReason()).isEqualTo("Kein Bedarf mehr");
    }

    @Test
    void removeWaitlistPlaceNotifiesTheUser() {
        found(waitlisted());

        service.remove(ORG_ID, OFFERING_ID, REGISTRATION_ID, "Doppelt angemeldet");

        ArgumentCaptor<CourseRegistrationRemovedEvent> captor =
                ArgumentCaptor.forClass(CourseRegistrationRemovedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue())
                .isEqualTo(
                        new CourseRegistrationRemovedEvent(
                                TENANT_ID,
                                USER_ID,
                                OFFERING_ID,
                                "Atemschutz",
                                "Doppelt angemeldet"));
    }

    @Test
    void removeRequiresReason() {
        assertThatThrownBy(() -> service.remove(ORG_ID, OFFERING_ID, REGISTRATION_ID, "  "))
                .isInstanceOf(CourseOfferingValidationException.class);
        verify(registrationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void removeRefusesOpenRegistrations() {
        CourseRegistration registration = pending();
        found(registration);

        assertThatThrownBy(() -> service.remove(ORG_ID, OFFERING_ID, REGISTRATION_ID, "Grund"))
                .isInstanceOf(CourseOfferingValidationException.class);
        assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.PENDING);
    }

    @Test
    void removeFreesTheSeatForTheNextApproval() {
        CourseRegistration participant = waitlisted();
        participant.setStatus(CourseRegistrationStatus.PARTICIPANT);
        found(participant);
        service.remove(ORG_ID, OFFERING_ID, REGISTRATION_ID, "Grund");

        CourseRegistration next = pending();
        when(registrationRepository.findByWorkflowId(WORKFLOW_ID)).thenReturn(Optional.of(next));
        participants(1);
        service.approve(WORKFLOW_ID);

        assertThat(next.getStatus()).isEqualTo(CourseRegistrationStatus.PARTICIPANT);
    }

    // --- cancellation by the user (workflow) ---

    @Test
    void requireCancellableAcceptsOwnParticipantBeforeFirstSession() {
        CourseRegistration registration = participant();
        ownRegistration(registration);

        CourseRegistrationService.CancellationCandidate candidate =
                service.requireCancellable(USER_ID, REGISTRATION_ID);

        assertThat(candidate.registration()).isSameAs(registration);
        assertThat(candidate.offering()).isSameAs(offering);
    }

    @Test
    void requireCancellableHidesRegistrationsOfOtherUsers() {
        when(registrationRepository.findByIdAndUserId(REGISTRATION_ID, USER_ID + 1))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireCancellable(USER_ID + 1, REGISTRATION_ID))
                .isInstanceOf(CourseRegistrationNotFoundException.class);
    }

    @Test
    void requireCancellableRefusesOpenRegistration() {
        CourseRegistration registration = participant();
        registration.setStatus(CourseRegistrationStatus.PENDING);
        ownRegistration(registration);

        assertThatThrownBy(() -> service.requireCancellable(USER_ID, REGISTRATION_ID))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("workflow");
    }

    @Test
    void requireCancellableRefusesWaitlistPlace() {
        CourseRegistration registration = participant();
        registration.setStatus(CourseRegistrationStatus.WAITLIST);
        ownRegistration(registration);

        assertThatThrownBy(() -> service.requireCancellable(USER_ID, REGISTRATION_ID))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("waitlist");
    }

    @Test
    void requireCancellableRefusesEndedRegistration() {
        CourseRegistration registration = participant();
        registration.setStatus(CourseRegistrationStatus.CANCELLED);
        ownRegistration(registration);

        assertThatThrownBy(() -> service.requireCancellable(USER_ID, REGISTRATION_ID))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("ended");
    }

    @Test
    void requireCancellableRefusesSecondParallelCancellation() {
        CourseRegistration registration = participant();
        registration.setCancellationWorkflowId(901L);
        ownRegistration(registration);

        assertThatThrownBy(() -> service.requireCancellable(USER_ID, REGISTRATION_ID))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("already open");
    }

    @Test
    void requireCancellableRefusesAfterFirstSessionStarted() {
        offering.setFirstSessionStart(Instant.now().minus(1, ChronoUnit.MINUTES));
        ownRegistration(participant());

        assertThatThrownBy(() -> service.requireCancellable(USER_ID, REGISTRATION_ID))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("started");
    }

    @Test
    void requireCancellableRefusesOfferingWithoutSessions() {
        offering.setFirstSessionStart(null);
        ownRegistration(participant());

        assertThatThrownBy(() -> service.requireCancellable(USER_ID, REGISTRATION_ID))
                .isInstanceOf(CourseOfferingValidationException.class);
    }

    @Test
    void requireCancellableRefusesOfferingThatIsNotPublished() {
        offering.setStatus(CourseOfferingStatus.COMPLETED);
        ownRegistration(participant());

        assertThatThrownBy(() -> service.requireCancellable(USER_ID, REGISTRATION_ID))
                .isInstanceOf(CourseOfferingValidationException.class);
    }

    @Test
    void canCancelMirrorsTheWorkflowRules() {
        Instant now = Instant.now();
        assertThat(CourseRegistrationService.canCancel(participant(), now)).isTrue();

        CourseRegistration withCancellation = participant();
        withCancellation.setCancellationWorkflowId(901L);
        assertThat(CourseRegistrationService.canCancel(withCancellation, now)).isFalse();

        CourseRegistration waitlist = participant();
        waitlist.setStatus(CourseRegistrationStatus.WAITLIST);
        assertThat(CourseRegistrationService.canCancel(waitlist, now)).isFalse();

        assertThat(
                        CourseRegistrationService.canCancel(
                                participant(), now.plus(40, ChronoUnit.DAYS)))
                .isFalse();
    }

    @Test
    void cancellationOpenedLinksWorkflowUnderOfferingLock() {
        CourseRegistration registration = participant();
        ownRegistration(registration);
        offeringIdOfOwnRegistration();

        service.cancellationOpened(USER_ID, REGISTRATION_ID, 901L);

        assertThat(registration.getCancellationWorkflowId()).isEqualTo(901L);
        assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.PARTICIPANT);
        InOrder order = inOrder(offeringRepository, registrationRepository);
        order.verify(offeringRepository).findForUpdate(OFFERING_ID);
        order.verify(registrationRepository).findByIdAndUserId(REGISTRATION_ID, USER_ID);
    }

    @Test
    void cancellationOpenedFailsWhenRegistrationIsNoLongerParticipant() {
        CourseRegistration registration = participant();
        registration.setStatus(CourseRegistrationStatus.CANCELLED);
        ownRegistration(registration);
        offeringIdOfOwnRegistration();

        assertThatThrownBy(() -> service.cancellationOpened(USER_ID, REGISTRATION_ID, 901L))
                .isInstanceOf(CourseOfferingValidationException.class);
        assertThat(registration.getCancellationWorkflowId()).isNull();
    }

    @Test
    void cancellationOpenedJudgesTheStateCommittedBeforeTheLockNotTheCachedOne() {
        // validateOpen loaded the registration as participant in the same transaction; the
        // organization removed it and committed before the lock was granted. The refresh under the
        // lock must reveal that, otherwise the stale participant would be written back.
        CourseRegistration registration = participant();
        ownRegistration(registration);
        offeringIdOfOwnRegistration();
        doAnswer(
                        invocation -> {
                            registration.setStatus(CourseRegistrationStatus.CANCELLED);
                            registration.setEndedBy(CourseRegistrationEndedBy.ORGANIZATION);
                            return null;
                        })
                .when(entityManager)
                .refresh(registration);

        assertThatThrownBy(() -> service.cancellationOpened(USER_ID, REGISTRATION_ID, 901L))
                .isInstanceOf(CourseOfferingValidationException.class);
        InOrder order = inOrder(offeringRepository, entityManager);
        order.verify(offeringRepository).findForUpdate(OFFERING_ID);
        order.verify(entityManager).refresh(registration);
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void approveJudgesTheStateCommittedBeforeTheLockNotTheCachedOne() {
        // The user cancelled the open registration and committed while the approval waited for
        // the lock; the approval must not turn the cancelled registration into a seat.
        CourseRegistration registration = pending();
        when(registrationRepository.findByWorkflowId(WORKFLOW_ID))
                .thenReturn(Optional.of(registration));
        doAnswer(
                        invocation -> {
                            registration.setStatus(CourseRegistrationStatus.CANCELLED);
                            return null;
                        })
                .when(entityManager)
                .refresh(registration);

        assertThatThrownBy(() -> service.approve(WORKFLOW_ID))
                .isInstanceOf(CourseOfferingValidationException.class);
        InOrder order = inOrder(offeringRepository, entityManager);
        order.verify(offeringRepository).findForUpdate(OFFERING_ID);
        order.verify(entityManager).refresh(registration);
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void cancellationOpenedFailsWhenAnotherCancellationIsOpen() {
        CourseRegistration registration = participant();
        registration.setCancellationWorkflowId(900L);
        ownRegistration(registration);
        offeringIdOfOwnRegistration();

        assertThatThrownBy(() -> service.cancellationOpened(USER_ID, REGISTRATION_ID, 901L))
                .isInstanceOf(CourseOfferingValidationException.class);
        assertThat(registration.getCancellationWorkflowId()).isEqualTo(900L);
    }

    @Test
    void newRegistrationIsRefusedWhileTheCancellationIsOpen() {
        // The participant place stays active until all deciders approved, so the unique active
        // registration rule keeps the user from registering again.
        CourseRegistration registration = participant();
        registration.setCancellationWorkflowId(901L);
        assertThat(registration.getStatus().isActive()).isTrue();
        eligible();
        when(registrationRepository.existsByOffering_IdAndUserIdAndStatusIn(
                        eq(OFFERING_ID), eq(USER_ID), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.requireRegistrable(USER_ID, OFFERING_ID))
                .isInstanceOf(CourseOfferingValidationException.class)
                .hasMessageContaining("already have an active registration");
    }

    @Test
    void approveCancellationEndsRegistrationAsUserCancelledAndFreesTheSeat() {
        CourseRegistration registration = participant();
        registration.setCancellationWorkflowId(901L);
        cancellationWorkflow(901L, registration);

        service.approveCancellation(901L);

        assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.CANCELLED);
        assertThat(registration.getEndedBy()).isEqualTo(CourseRegistrationEndedBy.USER);
        assertThat(registration.getEndedAt()).isNotNull();
        assertThat(registration.getEndReason()).isNull();
        // the workflow link stays as history
        assertThat(registration.getCancellationWorkflowId()).isEqualTo(901L);
        InOrder order = inOrder(offeringRepository, registrationRepository);
        order.verify(offeringRepository).findForUpdate(OFFERING_ID);
        order.verify(registrationRepository).findByCancellationWorkflowId(901L);
    }

    @Test
    void approveCancellationFreesSeatForNextApproval() {
        CourseRegistration leaving = participant();
        leaving.setCancellationWorkflowId(901L);
        cancellationWorkflow(901L, leaving);
        service.approveCancellation(901L);

        CourseRegistration next = pending();
        when(registrationRepository.findByWorkflowId(WORKFLOW_ID)).thenReturn(Optional.of(next));
        participants(1);
        service.approve(WORKFLOW_ID);

        assertThat(next.getStatus()).isEqualTo(CourseRegistrationStatus.PARTICIPANT);
    }

    @Test
    void approveCancellationLeavesRegistrationEndedByTheOrganizationAlone() {
        CourseRegistration registration = participant();
        registration.setStatus(CourseRegistrationStatus.CANCELLED);
        registration.setEndedBy(CourseRegistrationEndedBy.ORGANIZATION);
        registration.setCancellationWorkflowId(901L);
        cancellationWorkflow(901L, registration);

        service.approveCancellation(901L);

        assertThat(registration.getEndedBy()).isEqualTo(CourseRegistrationEndedBy.ORGANIZATION);
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void approveCancellationFailsForOfferingThatIsNoLongerPublished() {
        offering.setStatus(CourseOfferingStatus.COMPLETED);
        CourseRegistration registration = participant();
        registration.setCancellationWorkflowId(901L);
        cancellationWorkflow(901L, registration);

        assertThatThrownBy(() -> service.approveCancellation(901L))
                .isInstanceOf(CourseOfferingValidationException.class);
        assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.PARTICIPANT);
    }

    @Test
    void approveCancellationIgnoresUnknownWorkflow() {
        when(registrationRepository.findOfferingIdByCancellationWorkflow(901L))
                .thenReturn(Optional.empty());

        service.approveCancellation(901L);

        verify(registrationRepository, never()).save(any());
    }

    @Test
    void cancellationDeclinedKeepsRegistrationAndAllowsANewCancellation() {
        CourseRegistration registration = participant();
        registration.setCancellationWorkflowId(901L);
        cancellationWorkflow(901L, registration);

        service.cancellationDeclined(901L);

        assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.PARTICIPANT);
        assertThat(registration.getCancellationWorkflowId()).isNull();
        assertThat(registration.getEndedAt()).isNull();
        assertThat(CourseRegistrationService.canCancel(registration, Instant.now())).isTrue();
    }

    // --- waitlist withdrawal ---

    @Test
    void withdrawFromWaitlistEndsOwnWaitlistPlaceAsUserCancelled() {
        CourseRegistration registration = waitlisted();
        ownRegistration(registration);
        offeringIdOfOwnRegistration();

        service.withdrawFromWaitlist(USER_ID, REGISTRATION_ID);

        assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.CANCELLED);
        assertThat(registration.getEndedBy()).isEqualTo(CourseRegistrationEndedBy.USER);
        assertThat(registration.getEndedAt()).isNotNull();
        assertThat(registration.getEndReason()).isNull();
        verify(eventPublisher, never()).publishEvent(any(Object.class));
        InOrder order = inOrder(offeringRepository, registrationRepository);
        order.verify(offeringRepository).findForUpdate(OFFERING_ID);
        order.verify(registrationRepository).findByIdAndUserId(REGISTRATION_ID, USER_ID);
    }

    @Test
    void withdrawFromWaitlistRefusesParticipantsAndOpenRegistrations() {
        for (CourseRegistrationStatus status :
                List.of(
                        CourseRegistrationStatus.PARTICIPANT,
                        CourseRegistrationStatus.PENDING,
                        CourseRegistrationStatus.CANCELLED,
                        CourseRegistrationStatus.REJECTED)) {
            CourseRegistration registration = waitlisted();
            registration.setStatus(status);
            ownRegistration(registration);
            offeringIdOfOwnRegistration();

            assertThatThrownBy(() -> service.withdrawFromWaitlist(USER_ID, REGISTRATION_ID))
                    .isInstanceOf(CourseOfferingValidationException.class);
            assertThat(registration.getStatus()).isEqualTo(status);
        }
    }

    @Test
    void withdrawFromWaitlistHidesRegistrationsOfOtherUsers() {
        when(registrationRepository.findOfferingIdByIdAndUser(REGISTRATION_ID, USER_ID + 1))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.withdrawFromWaitlist(USER_ID + 1, REGISTRATION_ID))
                .isInstanceOf(CourseRegistrationNotFoundException.class);
        verify(registrationRepository, never()).save(any());
    }

    // --- quotas ---

    @Test
    void approveWithoutQuotasStoresNoAudienceEntry() {
        CourseRegistration registration = pending();
        when(registrationRepository.findByWorkflowId(WORKFLOW_ID))
                .thenReturn(Optional.of(registration));
        participants(0);

        service.approve(WORKFLOW_ID);

        assertThat(registration.getAudienceId()).isNull();
        verify(scopeService, never()).forUser(any());
    }

    @Test
    void approveTakesTheMostSpecificEntryWithAFreeSeat() {
        quotaOffering();
        userScope();
        CourseRegistration registration = approvable();
        taken(0, 0, 0, 0);

        service.approve(WORKFLOW_ID);

        assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.PARTICIPANT);
        assertThat(registration.getAudienceId()).isEqualTo(ORG_ENTRY);
    }

    @Test
    void approveFallsBackToTheNextSpecificEntryWhenTheSpecificQuotaIsUsedUp() {
        quotaOffering();
        userScope();
        CourseRegistration registration = approvable();
        taken(1, 0, 0, 0);

        service.approve(WORKFLOW_ID);

        assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.PARTICIPANT);
        assertThat(registration.getAudienceId()).isEqualTo(DEEP_SUBTREE_ENTRY);
    }

    @Test
    void approvePrefersTheDeeperSubtreeBeforeTheHigherOneAndTheTypeLast() {
        quotaOffering();
        userScope();
        CourseRegistration registration = approvable();
        taken(1, 2, 0, 0);

        service.approve(WORKFLOW_ID);
        assertThat(registration.getAudienceId()).isEqualTo(HIGH_SUBTREE_ENTRY);

        CourseRegistration next = approvable();
        taken(1, 2, 3, 0);
        service.approve(WORKFLOW_ID);
        assertThat(next.getAudienceId()).isEqualTo(TYPE_ENTRY);
        assertThat(next.getStatus()).isEqualTo(CourseRegistrationStatus.PARTICIPANT);
    }

    @Test
    void approveWithoutFreeSeatInAnyMatchingEntryJoinsTheMostSpecificWaitlist() {
        quotaOffering();
        userScope();
        CourseRegistration registration = approvable();
        when(registrationRepository.countSeatsByAudience(OFFERING_ID))
                .thenReturn(
                        List.of(
                                seats(ORG_ENTRY, CourseRegistrationStatus.PARTICIPANT, 1),
                                seats(DEEP_SUBTREE_ENTRY, CourseRegistrationStatus.PARTICIPANT, 2),
                                seats(HIGH_SUBTREE_ENTRY, CourseRegistrationStatus.PARTICIPANT, 3),
                                seats(TYPE_ENTRY, CourseRegistrationStatus.PARTICIPANT, 4)));
        participants(9);

        service.approve(WORKFLOW_ID);

        assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.WAITLIST);
        assertThat(registration.getAudienceId()).isEqualTo(ORG_ENTRY);
        assertThat(registration.getApprovedAt()).isNotNull();
    }

    @Test
    void approveIgnoresEntriesTheUserDoesNotMatch() {
        quotaOffering();
        // only the organization type matches
        when(scopeService.forUser(USER_ID))
                .thenReturn(
                        new CourseAudienceScope(
                                java.util.Set.of(99L),
                                java.util.Set.of(99L),
                                java.util.Set.of(TYPE_REF),
                                java.util.Map.of(99L, 1)));
        CourseRegistration registration = approvable();
        taken(0, 0, 0, 0);

        service.approve(WORKFLOW_ID);

        assertThat(registration.getAudienceId()).isEqualTo(TYPE_ENTRY);
    }

    @Test
    void approveFailsWhenTheUserMatchesNoEntryAnymore() {
        quotaOffering();
        when(scopeService.forUser(USER_ID))
                .thenReturn(
                        new CourseAudienceScope(
                                java.util.Set.of(99L),
                                java.util.Set.of(99L),
                                java.util.Set.of(),
                                java.util.Map.of(99L, 1)));
        CourseRegistration registration = approvable();

        assertThatThrownBy(() -> service.approve(WORKFLOW_ID))
                .isInstanceOf(CourseOfferingValidationException.class);
        assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.PENDING);
    }

    @Test
    void approveNeverExceedsTheOverallMaximumEvenWithFreeQuota() {
        quotaOffering();
        userScope();
        CourseRegistration registration = approvable();
        // inconsistent data: quotas free but the overall maximum (10) is reached
        participants(10);

        service.approve(WORKFLOW_ID);

        assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.WAITLIST);
    }

    @Test
    void approveReadsTheQuotaUsageOnlyAfterLockingTheOffering() {
        quotaOffering();
        userScope();
        approvable();
        taken(0, 0, 0, 0);

        service.approve(WORKFLOW_ID);

        InOrder order = inOrder(offeringRepository, registrationRepository);
        order.verify(offeringRepository).findForUpdate(OFFERING_ID);
        order.verify(registrationRepository).countSeatsByAudience(OFFERING_ID);
    }

    @Test
    void promoteRespectsTheQuotaOfTheEntry() {
        quotaOffering();
        CourseRegistration registration = waitlisted();
        registration.setAudienceId(ORG_ENTRY);
        found(registration);
        taken(1, 0, 0, 0);
        participants(1);

        assertThatThrownBy(() -> service.promote(ORG_ID, OFFERING_ID, REGISTRATION_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("quota");
        assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.WAITLIST);
    }

    @Test
    void promoteIgnoresFreeSeatsInOtherEntries() {
        quotaOffering();
        CourseRegistration registration = waitlisted();
        registration.setAudienceId(ORG_ENTRY);
        found(registration);
        // the subtree entries have room, the registration's own entry does not
        taken(1, 0, 0, 0);
        participants(1);

        assertThatThrownBy(() -> service.promote(ORG_ID, OFFERING_ID, REGISTRATION_ID))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void promoteMovesUpWhenTheQuotaHasRoomAndKeepsTheEntry() {
        quotaOffering();
        CourseRegistration registration = waitlisted();
        registration.setAudienceId(DEEP_SUBTREE_ENTRY);
        found(registration);
        taken(1, 1, 0, 0);
        participants(2);

        service.promote(ORG_ID, OFFERING_ID, REGISTRATION_ID);

        assertThat(registration.getStatus()).isEqualTo(CourseRegistrationStatus.PARTICIPANT);
        assertThat(registration.getAudienceId()).isEqualTo(DEEP_SUBTREE_ENTRY);
    }

    @Test
    void promoteStillRespectsTheOverallMaximumWithFreeQuota() {
        quotaOffering();
        CourseRegistration registration = waitlisted();
        registration.setAudienceId(TYPE_ENTRY);
        found(registration);
        participants(10);

        assertThatThrownBy(() -> service.promote(ORG_ID, OFFERING_ID, REGISTRATION_ID))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void promoteFailsForQuotaOfferingRegistrationWithoutEntry() {
        quotaOffering();
        found(waitlisted());
        participants(0);

        assertThatThrownBy(() -> service.promote(ORG_ID, OFFERING_ID, REGISTRATION_ID))
                .isInstanceOf(CourseOfferingValidationException.class);
    }

    @Test
    void removingAParticipantFreesTheSeatInItsQuota() {
        quotaOffering();
        userScope();
        CourseRegistration participant = waitlisted();
        participant.setStatus(CourseRegistrationStatus.PARTICIPANT);
        participant.setAudienceId(ORG_ENTRY);
        found(participant);
        service.remove(ORG_ID, OFFERING_ID, REGISTRATION_ID, "Grund");

        CourseRegistration next = approvable();
        // the removed participant no longer counts: the org entry is free again
        taken(0, 0, 0, 0);
        service.approve(WORKFLOW_ID);

        assertThat(participant.getAudienceId()).isEqualTo(ORG_ENTRY); // kept as history
        assertThat(next.getStatus()).isEqualTo(CourseRegistrationStatus.PARTICIPANT);
        assertThat(next.getAudienceId()).isEqualTo(ORG_ENTRY);
    }

    // --- helpers ---

    private static final long ORG_ENTRY = 201L;
    private static final long DEEP_SUBTREE_ENTRY = 202L;
    private static final long HIGH_SUBTREE_ENTRY = 203L;
    private static final long TYPE_ENTRY = 204L;
    private static final long TYPE_REF = 5L;

    /** Quotas 1 + 2 + 3 + 4 = 10. */
    private void quotaOffering() {
        offering.setMaxParticipants(10);
        // added in an order that differs from the specificity order
        entry(TYPE_ENTRY, CourseAudienceKind.ORG_TYPE, TYPE_REF, 4);
        entry(HIGH_SUBTREE_ENTRY, CourseAudienceKind.ORG_SUBTREE, 1L, 3);
        entry(ORG_ENTRY, CourseAudienceKind.ORG, ORG_ID, 1);
        entry(DEEP_SUBTREE_ENTRY, CourseAudienceKind.ORG_SUBTREE, 2L, 2);
    }

    private void entry(long id, CourseAudienceKind kind, Long refId, int quota) {
        offering.getAudiences()
                .add(
                        CourseOfferingAudience.builder()
                                .id(id)
                                .tenantId(TENANT_ID)
                                .offering(offering)
                                .kind(kind)
                                .refId(refId)
                                .quota(quota)
                                .build());
    }

    /** The user is a member of ORG_ID (child of 2, child of 1) and of the type TYPE_REF. */
    private void userScope() {
        when(scopeService.forUser(USER_ID))
                .thenReturn(
                        new CourseAudienceScope(
                                java.util.Set.of(ORG_ID),
                                java.util.Set.of(ORG_ID, 2L, 1L),
                                java.util.Set.of(TYPE_REF),
                                java.util.Map.of(ORG_ID, 2, 2L, 1, 1L, 0)));
    }

    private CourseRegistration approvable() {
        CourseRegistration registration = pending();
        when(registrationRepository.findByWorkflowId(WORKFLOW_ID))
                .thenReturn(Optional.of(registration));
        lenient()
                .when(
                        registrationRepository.countByOffering_IdAndStatus(
                                OFFERING_ID, CourseRegistrationStatus.PARTICIPANT))
                .thenReturn(0L);
        return registration;
    }

    /** Participants per entry: org, deep subtree, high subtree, type. */
    private void taken(long org, long deep, long high, long type) {
        when(registrationRepository.countSeatsByAudience(OFFERING_ID))
                .thenReturn(
                        List.of(
                                seats(ORG_ENTRY, CourseRegistrationStatus.PARTICIPANT, org),
                                seats(
                                        DEEP_SUBTREE_ENTRY,
                                        CourseRegistrationStatus.PARTICIPANT,
                                        deep),
                                seats(
                                        HIGH_SUBTREE_ENTRY,
                                        CourseRegistrationStatus.PARTICIPANT,
                                        high),
                                seats(TYPE_ENTRY, CourseRegistrationStatus.PARTICIPANT, type)));
    }

    private static CourseRegistrationRepository.AudienceSeatCount seats(
            Long audienceId, CourseRegistrationStatus status, long count) {
        return new CourseRegistrationRepository.AudienceSeatCount() {
            @Override
            public Long getAudienceId() {
                return audienceId;
            }

            @Override
            public CourseRegistrationStatus getStatus() {
                return status;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Specification<CourseOffering> anySpec() {
        return any(Specification.class);
    }

    private void eligible() {
        when(scopeService.forUser(USER_ID))
                .thenReturn(
                        new CourseAudienceScope(
                                java.util.Set.of(ORG_ID),
                                java.util.Set.of(ORG_ID),
                                java.util.Set.of()));
        when(offeringRepository.findOne(anySpec())).thenReturn(Optional.of(offering));
    }

    private void participants(long count) {
        when(registrationRepository.countByOffering_IdAndStatus(
                        OFFERING_ID, CourseRegistrationStatus.PARTICIPANT))
                .thenReturn(count);
    }

    private void found(CourseRegistration registration) {
        when(registrationRepository.findByIdAndOffering_Id(REGISTRATION_ID, OFFERING_ID))
                .thenReturn(Optional.of(registration));
    }

    private void ownRegistration(CourseRegistration registration) {
        lenient()
                .when(registrationRepository.findByIdAndUserId(REGISTRATION_ID, USER_ID))
                .thenReturn(Optional.of(registration));
    }

    private void offeringIdOfOwnRegistration() {
        when(registrationRepository.findOfferingIdByIdAndUser(REGISTRATION_ID, USER_ID))
                .thenReturn(Optional.of(OFFERING_ID));
    }

    private void cancellationWorkflow(long workflowId, CourseRegistration registration) {
        when(registrationRepository.findOfferingIdByCancellationWorkflow(workflowId))
                .thenReturn(Optional.of(OFFERING_ID));
        when(registrationRepository.findByCancellationWorkflowId(workflowId))
                .thenReturn(Optional.of(registration));
    }

    private CourseRegistration participant() {
        CourseRegistration registration = pending();
        registration.setStatus(CourseRegistrationStatus.PARTICIPANT);
        registration.setApprovedAt(Instant.parse("2027-01-01T10:00:00Z"));
        return registration;
    }

    private CourseRegistration pending() {
        return CourseRegistration.builder()
                .id(REGISTRATION_ID)
                .tenantId(TENANT_ID)
                .offering(offering)
                .userId(USER_ID)
                .workflowId(WORKFLOW_ID)
                .status(CourseRegistrationStatus.PENDING)
                .registeredAt(Instant.parse("2027-01-01T09:00:00Z"))
                .build();
    }

    private CourseRegistration waitlisted() {
        CourseRegistration registration = pending();
        registration.setStatus(CourseRegistrationStatus.WAITLIST);
        registration.setApprovedAt(Instant.parse("2027-01-01T10:00:00Z"));
        return registration;
    }

    private static CourseOffering publishedOffering(int maxParticipants) {
        return CourseOffering.builder()
                .id(OFFERING_ID)
                .tenantId(TENANT_ID)
                .organizationId(ORG_ID)
                .trainingCourseTypeId(TYPE_ID)
                .status(CourseOfferingStatus.PUBLISHED)
                .registrationStart(Instant.now().minus(1, ChronoUnit.DAYS))
                .registrationEnd(Instant.now().plus(1, ChronoUnit.DAYS))
                .maxParticipants(maxParticipants)
                .firstSessionStart(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();
    }
}
