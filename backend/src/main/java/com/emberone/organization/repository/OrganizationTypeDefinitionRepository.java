/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.repository;

import com.emberone.organization.domain.OrganizationTypeDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface OrganizationTypeDefinitionRepository
        extends JpaRepository<OrganizationTypeDefinition, Long> {

    List<OrganizationTypeDefinition> findAllByOrderByNameAsc();

    Optional<OrganizationTypeDefinition> findByIdAndActiveTrue(Long id);

    Optional<OrganizationTypeDefinition> findByConfigKey(String configKey);

    boolean existsByNameIgnoreCase(String name);
}
