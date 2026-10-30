/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.repository;

import com.emberone.admin.tenant.domain.Tenant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface TenantRepository
        extends JpaRepository<Tenant, Long>, JpaSpecificationExecutor<Tenant> {

    boolean existsByAbbreviation(String abbreviation);

    @Query("SELECT t.id FROM Tenant t ORDER BY t.id")
    List<Long> findAllIdsOrderByIdAsc();
}
