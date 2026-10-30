/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.repository;

import com.emberone.standard.domain.StandardModule;
import com.emberone.standard.domain.StandardModulePK;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface StandardModuleRepository extends JpaRepository<StandardModule, StandardModulePK> {

    // Explicit tenant filter: loader of EnabledModulesCache, cached under a tenantId key.
    List<StandardModule> findAllByTenantIdAndStandardId(Long tenantId, Long standardId);

    @Transactional
    void deleteAllByStandardId(Long standardId);
}
