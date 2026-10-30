/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.tenant;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface TenantQualificationDefinitionRepository
        extends JpaRepository<TenantQualificationDefinition, Long> {

    List<TenantQualificationDefinition> findAllByOrderByNameAsc();

    Optional<TenantQualificationDefinition> findByConfigKey(String configKey);

    List<TenantQualificationDefinition> findByConfigKeyIn(List<String> configKeys);

    boolean existsByConfigKeyAndArchivedFalse(String configKey);

    boolean existsByConfigKeyAndArchivedFalseAndIdNot(String configKey, Long id);
}
