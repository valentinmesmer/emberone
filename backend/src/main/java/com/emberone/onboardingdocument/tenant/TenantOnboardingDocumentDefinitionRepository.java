/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.tenant;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface TenantOnboardingDocumentDefinitionRepository
        extends JpaRepository<TenantOnboardingDocumentDefinition, Long> {

    List<TenantOnboardingDocumentDefinition> findAllByOrderByNameAsc();

    Optional<TenantOnboardingDocumentDefinition> findByConfigKey(String configKey);

    boolean existsByConfigKeyAndArchivedFalse(String configKey);

    boolean existsByConfigKeyAndArchivedFalseAndIdNot(String configKey, Long id);
}
