/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.repository;

import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.trainingcourse.domain.TrainingCourseUserSubmission;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface TrainingCourseUserSubmissionRepository
        extends JpaRepository<TrainingCourseUserSubmission, Long>,
                JpaSpecificationExecutor<TrainingCourseUserSubmission> {

    @EntityGraph(attributePaths = "grantedTypes")
    List<TrainingCourseUserSubmission> findByUserId(Long userId);

    @EntityGraph(attributePaths = "grantedTypes")
    @Query("SELECT s FROM TrainingCourseUserSubmission s WHERE s.id IN :ids")
    List<TrainingCourseUserSubmission> findWithGrantedTypesByIdIn(
            @Param("ids") Collection<Long> ids);

    Optional<TrainingCourseUserSubmission> findByAppLinkTypeAndAppLinkId(
            AppLinkType appLinkType, Long appLinkId);

    @Query(
            "SELECT DISTINCT g.tenantTrainingCourseTypeId FROM TrainingCourseUserSubmissionGrantedType g "
                    + "WHERE g.submission.userId = :userId")
    List<Long> findGrantedTypeIdsByUser(@Param("userId") Long userId);

    boolean existsByGrantedTypes_TenantTrainingCourseTypeId(Long tenantTrainingCourseTypeId);
}
