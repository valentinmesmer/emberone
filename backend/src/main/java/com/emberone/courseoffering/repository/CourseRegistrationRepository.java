/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.repository;

import com.emberone.courseoffering.domain.CourseRegistration;
import com.emberone.courseoffering.domain.CourseRegistrationStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface CourseRegistrationRepository
        extends JpaRepository<CourseRegistration, Long>,
                JpaSpecificationExecutor<CourseRegistration> {

    Optional<CourseRegistration> findByWorkflowId(Long workflowId);

    Optional<CourseRegistration> findByIdAndOffering_Id(Long id, Long offeringId);

    List<CourseRegistration> findByOffering_IdAndStatusOrderByApprovedAtAscIdAsc(
            Long offeringId, CourseRegistrationStatus status);

    List<CourseRegistration> findByOffering_IdAndIdIn(Long offeringId, Collection<Long> ids);

    Optional<CourseRegistration> findByCancellationWorkflowId(Long cancellationWorkflowId);

    Optional<CourseRegistration> findByIdAndUserId(Long id, Long userId);

    boolean existsByOffering_IdAndUserId(Long offeringId, Long userId);

    boolean existsByOffering_IdAndUserIdAndStatusIn(
            Long offeringId, Long userId, Collection<CourseRegistrationStatus> statuses);

    List<CourseRegistration> findByUserIdAndOffering_IdInAndStatusIn(
            Long userId,
            Collection<Long> offeringIds,
            Collection<CourseRegistrationStatus> statuses);

    /** The offering of a user's own registration, to lock it before reading the registration. */
    @Query(
            """
            SELECT r.offering.id FROM CourseRegistration r
            WHERE r.id = :id AND r.userId = :userId
            """)
    Optional<Long> findOfferingIdByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    /** The offering of a registration with this cancellation workflow, to lock it first. */
    @Query(
            """
            SELECT r.offering.id FROM CourseRegistration r
            WHERE r.cancellationWorkflowId = :workflowId
            """)
    Optional<Long> findOfferingIdByCancellationWorkflow(@Param("workflowId") Long workflowId);

    List<CourseRegistration> findByOffering_IdAndStatusIn(
            Long offeringId, Collection<CourseRegistrationStatus> statuses);

    /** Users holding a registration of the offering in one of the statuses. */
    @Query(
            """
            SELECT DISTINCT r.userId FROM CourseRegistration r
            WHERE r.offering.id = :offeringId
              AND r.status IN :statuses
            """)
    List<Long> findUserIdsByOfferingAndStatusIn(
            @Param("offeringId") Long offeringId,
            @Param("statuses") Collection<CourseRegistrationStatus> statuses);

    long countByOffering_IdAndStatus(Long offeringId, CourseRegistrationStatus status);

    /** Waitlist of an offering in queue order; positions are counted per audience entry. */
    @Query(
            """
            SELECT r.id AS id, r.audienceId AS audienceId FROM CourseRegistration r
            WHERE r.offering.id = :offeringId
              AND r.status = com.emberone.courseoffering.domain.CourseRegistrationStatus.WAITLIST
            ORDER BY r.approvedAt ASC, r.id ASC
            """)
    List<WaitlistEntry> findWaitlistInOrder(@Param("offeringId") Long offeringId);

    /** Participants and waitlist places per audience entry (entry null: no quotas). */
    @Query(
            """
            SELECT r.audienceId AS audienceId, r.status AS status, COUNT(r) AS count
            FROM CourseRegistration r
            WHERE r.offering.id = :offeringId
              AND r.status IN (
                  com.emberone.courseoffering.domain.CourseRegistrationStatus.PARTICIPANT,
                  com.emberone.courseoffering.domain.CourseRegistrationStatus.WAITLIST)
            GROUP BY r.audienceId, r.status
            """)
    List<AudienceSeatCount> countSeatsByAudience(@Param("offeringId") Long offeringId);

    /** Participants of the given audience entries, to tell whether a quota is used up. */
    @Query(
            """
            SELECT r.audienceId AS audienceId, r.status AS status, COUNT(r) AS count
            FROM CourseRegistration r
            WHERE r.audienceId IN :audienceIds
              AND r.status = com.emberone.courseoffering.domain.CourseRegistrationStatus.PARTICIPANT
            GROUP BY r.audienceId, r.status
            """)
    List<AudienceSeatCount> countParticipantsByAudience(
            @Param("audienceIds") Collection<Long> audienceIds);

    @Query(
            """
            SELECT r.status AS status, COUNT(r) AS count FROM CourseRegistration r
            WHERE r.offering.id = :offeringId
            GROUP BY r.status
            """)
    List<StatusCount> countByStatus(@Param("offeringId") Long offeringId);

    @Query(
            """
            SELECT r.offering.id AS offeringId, COUNT(r) AS count FROM CourseRegistration r
            WHERE r.offering.id IN :offeringIds
              AND r.status = com.emberone.courseoffering.domain.CourseRegistrationStatus.PARTICIPANT
            GROUP BY r.offering.id
            """)
    List<ParticipantCount> countParticipants(@Param("offeringIds") Collection<Long> offeringIds);

    interface WaitlistEntry {
        Long getId();

        Long getAudienceId();
    }

    interface AudienceSeatCount {
        Long getAudienceId();

        CourseRegistrationStatus getStatus();

        long getCount();
    }

    interface StatusCount {
        CourseRegistrationStatus getStatus();

        long getCount();
    }

    interface ParticipantCount {
        Long getOfferingId();

        long getCount();
    }
}
