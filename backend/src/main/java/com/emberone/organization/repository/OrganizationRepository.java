/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.repository;

import com.emberone.organization.domain.Organization;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface OrganizationRepository
        extends JpaRepository<Organization, Long>, JpaSpecificationExecutor<Organization> {

    List<Organization> findByParentOrganizationId(Long parentOrganizationId);

    /**
     * Organizations of a type that inherit their org-type standard (no explicit per-org override).
     */
    List<Organization> findByType_IdAndStandardIdIsNull(Long organizationTypeId);

    boolean existsByType_Id(Long organizationTypeId);

    /** Organizations with an explicit per-org standard override pointing at {@code standardId}. */
    // Explicit tenant filter: loader of StandardOrganizationIdCache, cached under a tenantId key.
    List<Organization> findByTenantIdAndStandardId(Long tenantId, Long standardId);

    /**
     * Organizations with an explicit per-org standard override (instead of inheriting their type
     * default).
     */
    List<Organization> findByStandardIdIsNotNull();

    boolean existsByStandardId(Long standardId);

    List<Organization> findByParentOrganizationIsNull();

    Optional<Organization> findByOrganizationCode(String organizationCode);

    boolean existsByParentOrganization_Id(Long parentOrganizationId);

    // Explicit tenant filter: checks a caller-supplied tenant id, cached per (organizationId,
    // tenantId) in OrganizationTenantCache; organizations has the global-admin read policy.
    boolean existsByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT DISTINCT o FROM Organization o LEFT JOIN FETCH o.parentOrganization")
    List<Organization> findAllWithParentFetch();

    @Query(
            "SELECT o FROM Organization o WHERE o.deactivatedAt IS NULL ORDER BY o.name ASC, o.id ASC")
    List<Organization> findAllActiveOrderByName();

    // Explicit tenant filter: also called on cross-tenant admin read routes
    // (AdminOrganizationIdFilterOptionsProvider); organizations has the global-admin read policy.
    @Query(
            "SELECT o FROM Organization o WHERE o.tenantId = :tenantId AND o.deactivatedAt IS NULL ORDER BY o.name ASC, o.id ASC")
    List<Organization> findActiveByTenantIdOrderByName(@Param("tenantId") Long tenantId);
}
