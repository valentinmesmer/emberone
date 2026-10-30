/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.dutyfunction.tenant;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface TenantFunctionRepository extends JpaRepository<TenantFunction, Long> {

    List<TenantFunction> findAllByOrderBySortOrderAscNameAsc();

    Optional<TenantFunction> findByConfigKey(String configKey);

    boolean existsByConfigKeyAndArchivedFalse(String configKey);
}
