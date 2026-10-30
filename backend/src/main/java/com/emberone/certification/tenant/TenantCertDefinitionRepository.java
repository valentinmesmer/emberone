/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.tenant;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface TenantCertDefinitionRepository extends JpaRepository<TenantCertDefinition, Long> {

    List<TenantCertDefinition> findAllByOrderByNameAsc();

    Optional<TenantCertDefinition> findByConfigKey(String configKey);

    List<TenantCertDefinition> findByConfigKeyIn(Collection<String> configKeys);

    boolean existsByConfigKeyAndArchivedFalse(String configKey);

    boolean existsByConfigKeyAndArchivedFalseAndIdNot(String configKey, Long id);
}
