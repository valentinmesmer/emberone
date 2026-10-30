/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.tenant;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface TenantQualificationTrainingCourseRepository
        extends JpaRepository<TenantQualificationTrainingCourse, Long> {

    List<TenantQualificationTrainingCourse> findByTrainingCourseConfigKey(
            String trainingCourseConfigKey);

    @Transactional
    void deleteByTrainingCourseConfigKey(String trainingCourseConfigKey);
}
