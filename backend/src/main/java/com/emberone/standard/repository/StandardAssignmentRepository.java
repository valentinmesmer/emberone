/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.repository;

import com.emberone.standard.domain.StandardAssignment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface StandardAssignmentRepository
        extends JpaRepository<StandardAssignment, Long>,
                JpaSpecificationExecutor<StandardAssignment> {

    // Explicit tenant filter: loader of EffectiveStandardTypeCache, cached under a tenantId key.
    Optional<StandardAssignment> findByTenantIdAndOrganizationType_Id(
            Long tenantId, Long organizationTypeId);

    // Explicit tenant filter: loader of StandardOrganizationIdCache, cached under a tenantId key.
    List<StandardAssignment> findAllByTenantIdAndStandardId(Long tenantId, Long standardId);

    boolean existsByStandardId(Long standardId);

    boolean existsByOrganizationType_Id(Long organizationTypeId);
}
