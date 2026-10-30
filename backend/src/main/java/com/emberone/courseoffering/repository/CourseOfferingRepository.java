/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.repository;

import com.emberone.courseoffering.domain.CourseOffering;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface CourseOfferingRepository
        extends JpaRepository<CourseOffering, Long>, JpaSpecificationExecutor<CourseOffering> {

    Optional<CourseOffering> findByIdAndOrganizationId(Long id, Long organizationId);

    boolean existsByTrainingCourseTypeId(Long trainingCourseTypeId);

    @Query(
            """
            SELECT DISTINCT o.trainingCourseTypeId FROM CourseOffering o
            WHERE o.organizationId = :organizationId
            ORDER BY o.trainingCourseTypeId
            """)
    List<Long> findDistinctTrainingCourseTypeIdsByOrganization(
            @Param("organizationId") Long organizationId);

    /**
     * Loads the offering with a pessimistic write lock. Every operation that changes seat usage
     * takes this lock first, so concurrent approvals and move-ups serialize per offering and can
     * never overbook it.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM CourseOffering o WHERE o.id = :id")
    Optional<CourseOffering> findForUpdate(@Param("id") Long id);

    /**
     * Published offerings that start within the lead time and whose start reminder has not gone out
     * yet.
     */
    @Query(
            """
            SELECT o.id FROM CourseOffering o
            WHERE o.status = com.emberone.courseoffering.domain.CourseOfferingStatus.PUBLISHED
              AND o.startReminderSentAt IS NULL
              AND o.firstSessionStart > :now
              AND o.firstSessionStart <= :until
            ORDER BY o.id
            """)
    List<Long> findIdsDueForStartReminder(@Param("now") Instant now, @Param("until") Instant until);

    List<CourseOffering> findByIdIn(List<Long> ids);
}
