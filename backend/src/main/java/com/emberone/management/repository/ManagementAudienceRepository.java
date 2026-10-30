/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.repository;

import com.emberone.management.domain.ManagementAudience;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface ManagementAudienceRepository extends JpaRepository<ManagementAudience, Long> {

    List<ManagementAudience> findByManagingOrganizationIdOrderByIdAsc(Long managingOrganizationId);

    @Query("SELECT DISTINCT a.managingOrganizationId FROM ManagementAudience a")
    List<Long> findDistinctManagingOrganizationIds();
}
