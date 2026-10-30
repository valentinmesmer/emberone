/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permissiongroup.standard;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface PermissionGroupTemplateRepository
        extends JpaRepository<PermissionGroupTemplate, Long> {

    @EntityGraph(attributePaths = "grants")
    List<PermissionGroupTemplate> findByStandardId(Long standardId);

    @EntityGraph(attributePaths = "grants")
    Optional<PermissionGroupTemplate> findByStandardIdAndConfigKey(
            Long standardId, String configKey);

    boolean existsByStandardIdAndConfigKey(Long standardId, String configKey);
}
