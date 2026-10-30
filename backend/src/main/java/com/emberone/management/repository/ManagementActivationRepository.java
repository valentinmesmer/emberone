/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.repository;

import com.emberone.management.domain.ManagementActivation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface ManagementActivationRepository
        extends JpaRepository<ManagementActivation, Long>,
                JpaSpecificationExecutor<ManagementActivation> {

    @Override
    @EntityGraph(attributePaths = {"user", "managingOrganization", "targetOrganization"})
    Page<ManagementActivation> findAll(Specification<ManagementActivation> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "managingOrganization", "targetOrganization"})
    Optional<ManagementActivation> findWithDetailsById(Long id);

    Optional<ManagementActivation> findByIdAndUserIdAndEndedAtIsNull(Long id, Long userId);

    Optional<ManagementActivation> findByUserIdAndTargetOrganizationIdAndEndedAtIsNull(
            Long userId, Long targetOrganizationId);

    // Explicit tenant filter: feeds permission evaluation (ManagementAccessService);
    // org_management_activations has the global-admin read policy.
    List<ManagementActivation>
            findByTenantIdAndUserIdAndEndedAtIsNullAndValidUntilAfterOrderByValidUntilAsc(
                    Long tenantId, Long userId, Instant now);

    /**
     * Atomically marks open, expired activations as notified and returns their ids. Rows locked by
     * another instance are skipped, so each activation is claimed exactly once.
     */
    @Transactional
    @Query(
            value =
                    """
            WITH picked AS (
                SELECT a.id FROM org_management_activations a
                WHERE a.ended_at IS NULL
                  AND a.expiry_notified_at IS NULL
                  AND a.valid_until <= :now
                ORDER BY a.id
                FOR UPDATE SKIP LOCKED
            )
            UPDATE org_management_activations u
            SET expiry_notified_at = :now
            FROM picked
            WHERE u.id = picked.id
            RETURNING u.id
            """,
            nativeQuery = true)
    List<Long> claimExpiredForNotification(@Param("now") Instant now);

    List<ManagementActivation> findByManagingOrganizationIdAndEndedAtIsNull(
            Long managingOrganizationId);
}
