/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emberone.courseoffering.domain.CourseAudienceKind;
import com.emberone.courseoffering.domain.CourseOffering;
import com.emberone.courseoffering.domain.CourseOfferingAudience;
import com.emberone.courseoffering.domain.CourseOfferingStatus;
import com.emberone.courseoffering.domain.CourseRegistration;
import com.emberone.courseoffering.domain.CourseRegistrationStatus;
import com.emberone.courseoffering.repository.CourseOfferingRepository;
import com.emberone.courseoffering.repository.CourseRegistrationRepository;
import com.emberone.platform.security.TokenService;
import com.emberone.trainingcourse.repository.TrainingCourseUserSubmissionRepository;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Many approvals for one offering at once must never exceed the maximum. The database row lock
 * ({@code SELECT ... FOR UPDATE}, held until the transaction ends) is simulated by a lock that
 * {@code findForUpdate} takes and the test releases after the call returns, standing in for the
 * commit. The slowed-down seat count would let concurrent approvals overbook if the service counted
 * seats before taking that lock.
 */
class CourseRegistrationCapacityConcurrencyTest {

    private static final long TENANT_ID = 7L;
    private static final long OFFERING_ID = 100L;
    private static final int MAX_PARTICIPANTS = 3;
    private static final int APPROVALS = 12;

    @Test
    void concurrentApprovalsNeverExceedMaxParticipants() throws Exception {
        CourseOffering offering =
                CourseOffering.builder()
                        .id(OFFERING_ID)
                        .tenantId(TENANT_ID)
                        .organizationId(10L)
                        .status(CourseOfferingStatus.PUBLISHED)
                        .maxParticipants(MAX_PARTICIPANTS)
                        .build();
        Map<Long, CourseRegistration> table = new ConcurrentHashMap<>();
        for (long workflowId = 1; workflowId <= APPROVALS; workflowId++) {
            table.put(
                    workflowId,
                    CourseRegistration.builder()
                            .id(workflowId)
                            .tenantId(TENANT_ID)
                            .offering(offering)
                            .userId(1000 + workflowId)
                            .workflowId(workflowId)
                            .status(CourseRegistrationStatus.PENDING)
                            .registeredAt(Instant.now())
                            .build());
        }
        ReentrantLock rowLock = new ReentrantLock();

        CourseOfferingRepository offeringRepository = mock(CourseOfferingRepository.class);
        when(offeringRepository.findForUpdate(OFFERING_ID))
                .thenAnswer(
                        invocation -> {
                            rowLock.lock();
                            return Optional.of(offering);
                        });
        CourseRegistrationRepository registrationRepository =
                mock(CourseRegistrationRepository.class);
        when(registrationRepository.findByWorkflowId(anyLong()))
                .thenAnswer(
                        invocation ->
                                Optional.ofNullable(table.get(invocation.<Long>getArgument(0))));
        when(registrationRepository.countByOffering_IdAndStatus(
                        OFFERING_ID, CourseRegistrationStatus.PARTICIPANT))
                .thenAnswer(
                        invocation -> {
                            long count =
                                    table.values().stream()
                                            .filter(
                                                    r ->
                                                            r.getStatus()
                                                                    == CourseRegistrationStatus
                                                                            .PARTICIPANT)
                                            .count();
                            Thread.sleep(5);
                            return count;
                        });
        when(registrationRepository.save(any(CourseRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CourseRegistrationService service =
                new CourseRegistrationService(
                        offeringRepository,
                        registrationRepository,
                        mock(CourseAudienceScopeService.class),
                        mock(CoursePrerequisiteService.class),
                        mock(TrainingCourseUserSubmissionRepository.class),
                        mock(CourseRegistrationAssembler.class),
                        mock(TokenService.class),
                        mock(ApplicationEventPublisher.class),
                        mock(EntityManager.class),
                        mock(TenantTrainingCourseTypeRepository.class));

        ExecutorService executor = Executors.newFixedThreadPool(APPROVALS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> results = new ArrayList<>();
        for (long workflowId = 1; workflowId <= APPROVALS; workflowId++) {
            long id = workflowId;
            results.add(
                    executor.submit(
                            () -> {
                                start.await();
                                try {
                                    service.approve(id);
                                } finally {
                                    if (rowLock.isHeldByCurrentThread()) {
                                        rowLock.unlock();
                                    }
                                }
                                return null;
                            }));
        }
        start.countDown();
        for (Future<?> result : results) {
            result.get(30, TimeUnit.SECONDS);
        }
        executor.shutdownNow();

        assertThat(count(table, CourseRegistrationStatus.PARTICIPANT)).isEqualTo(MAX_PARTICIPANTS);
        assertThat(count(table, CourseRegistrationStatus.WAITLIST))
                .isEqualTo(APPROVALS - MAX_PARTICIPANTS);
        assertThat(count(table, CourseRegistrationStatus.PENDING)).isZero();
    }

    /**
     * With quotas, concurrent approvals of users matching two entries must neither exceed an
     * entry's quota nor the overall maximum: 2 + 1 seats, 12 approvals of users who match both
     * entries. The seat count per entry is slowed down like in the test above.
     */
    @Test
    void concurrentApprovalsNeverExceedAnyQuota() throws Exception {
        CourseOffering offering =
                CourseOffering.builder()
                        .id(OFFERING_ID)
                        .tenantId(TENANT_ID)
                        .organizationId(10L)
                        .status(CourseOfferingStatus.PUBLISHED)
                        .maxParticipants(MAX_PARTICIPANTS)
                        .build();
        CourseOfferingAudience specific = quotaEntry(offering, 1L, CourseAudienceKind.ORG, 10L, 2);
        CourseOfferingAudience general =
                quotaEntry(offering, 2L, CourseAudienceKind.ORG_TYPE, 5L, 1);
        offering.getAudiences().add(general);
        offering.getAudiences().add(specific);
        Map<Long, CourseRegistration> table = new ConcurrentHashMap<>();
        for (long workflowId = 1; workflowId <= APPROVALS; workflowId++) {
            table.put(
                    workflowId,
                    CourseRegistration.builder()
                            .id(workflowId)
                            .tenantId(TENANT_ID)
                            .offering(offering)
                            .userId(1000 + workflowId)
                            .workflowId(workflowId)
                            .status(CourseRegistrationStatus.PENDING)
                            .registeredAt(Instant.now())
                            .build());
        }
        ReentrantLock rowLock = new ReentrantLock();

        CourseOfferingRepository offeringRepository = mock(CourseOfferingRepository.class);
        when(offeringRepository.findForUpdate(OFFERING_ID))
                .thenAnswer(
                        invocation -> {
                            rowLock.lock();
                            return Optional.of(offering);
                        });
        CourseRegistrationRepository registrationRepository =
                mock(CourseRegistrationRepository.class);
        when(registrationRepository.findByWorkflowId(anyLong()))
                .thenAnswer(
                        invocation ->
                                Optional.ofNullable(table.get(invocation.<Long>getArgument(0))));
        when(registrationRepository.countByOffering_IdAndStatus(
                        OFFERING_ID, CourseRegistrationStatus.PARTICIPANT))
                .thenAnswer(invocation -> count(table, CourseRegistrationStatus.PARTICIPANT));
        when(registrationRepository.countSeatsByAudience(OFFERING_ID))
                .thenAnswer(
                        invocation -> {
                            List<CourseRegistrationRepository.AudienceSeatCount> rows =
                                    new ArrayList<>();
                            for (long audienceId : List.of(1L, 2L)) {
                                long taken =
                                        table.values().stream()
                                                .filter(
                                                        r ->
                                                                r.getStatus()
                                                                                == CourseRegistrationStatus
                                                                                        .PARTICIPANT
                                                                        && Long.valueOf(audienceId)
                                                                                .equals(
                                                                                        r
                                                                                                .getAudienceId()))
                                                .count();
                                rows.add(seatCount(audienceId, taken));
                            }
                            Thread.sleep(5);
                            return rows;
                        });
        when(registrationRepository.save(any(CourseRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CourseAudienceScopeService scopeService = mock(CourseAudienceScopeService.class);
        when(scopeService.forUser(anyLong()))
                .thenReturn(
                        new CourseAudienceScope(
                                Set.of(10L), Set.of(10L), Set.of(5L), Map.of(10L, 0)));
        CourseRegistrationService service =
                new CourseRegistrationService(
                        offeringRepository,
                        registrationRepository,
                        scopeService,
                        mock(CoursePrerequisiteService.class),
                        mock(TrainingCourseUserSubmissionRepository.class),
                        mock(CourseRegistrationAssembler.class),
                        mock(TokenService.class),
                        mock(ApplicationEventPublisher.class),
                        mock(EntityManager.class),
                        mock(TenantTrainingCourseTypeRepository.class));

        ExecutorService executor = Executors.newFixedThreadPool(APPROVALS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> results = new ArrayList<>();
        for (long workflowId = 1; workflowId <= APPROVALS; workflowId++) {
            long id = workflowId;
            results.add(
                    executor.submit(
                            () -> {
                                start.await();
                                try {
                                    service.approve(id);
                                } finally {
                                    if (rowLock.isHeldByCurrentThread()) {
                                        rowLock.unlock();
                                    }
                                }
                                return null;
                            }));
        }
        start.countDown();
        for (Future<?> result : results) {
            result.get(30, TimeUnit.SECONDS);
        }
        executor.shutdownNow();

        assertThat(count(table, CourseRegistrationStatus.PARTICIPANT)).isEqualTo(MAX_PARTICIPANTS);
        assertThat(participantsOf(table, 1L)).isEqualTo(2);
        assertThat(participantsOf(table, 2L)).isEqualTo(1);
        // everyone left over waits on the most specific entry
        assertThat(count(table, CourseRegistrationStatus.WAITLIST))
                .isEqualTo(APPROVALS - MAX_PARTICIPANTS);
        assertThat(
                        table.values().stream()
                                .filter(r -> r.getStatus() == CourseRegistrationStatus.WAITLIST)
                                .map(CourseRegistration::getAudienceId)
                                .distinct()
                                .toList())
                .containsExactly(1L);
    }

    private static long participantsOf(Map<Long, CourseRegistration> table, long audienceId) {
        return table.values().stream()
                .filter(
                        r ->
                                r.getStatus() == CourseRegistrationStatus.PARTICIPANT
                                        && Long.valueOf(audienceId).equals(r.getAudienceId()))
                .count();
    }

    private static CourseOfferingAudience quotaEntry(
            CourseOffering offering, long id, CourseAudienceKind kind, long refId, int quota) {
        return CourseOfferingAudience.builder()
                .id(id)
                .tenantId(TENANT_ID)
                .offering(offering)
                .kind(kind)
                .refId(refId)
                .quota(quota)
                .build();
    }

    private static CourseRegistrationRepository.AudienceSeatCount seatCount(
            long audienceId, long count) {
        return new CourseRegistrationRepository.AudienceSeatCount() {
            @Override
            public Long getAudienceId() {
                return audienceId;
            }

            @Override
            public CourseRegistrationStatus getStatus() {
                return CourseRegistrationStatus.PARTICIPANT;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }

    private static long count(
            Map<Long, CourseRegistration> table, CourseRegistrationStatus status) {
        return table.values().stream().filter(r -> r.getStatus() == status).count();
    }
}
