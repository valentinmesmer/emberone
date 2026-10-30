/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.tenant;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface TenantTrainingCourseTypeRepository
        extends JpaRepository<TenantTrainingCourseType, Long> {

    List<TenantTrainingCourseType> findAllByOrderByNameAsc();

    Optional<TenantTrainingCourseType> findByConfigKey(String configKey);

    List<TenantTrainingCourseType> findByConfigKeyIn(Collection<String> configKeys);

    boolean existsByConfigKeyAndArchivedFalse(String configKey);

    boolean existsByConfigKeyAndArchivedFalseAndIdNot(String configKey, Long id);
}
