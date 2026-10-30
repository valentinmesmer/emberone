/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.repository;

import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import java.util.Collection;
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
public interface OrganizationGroupRepository
        extends JpaRepository<OrganizationGroup, Long>,
                JpaSpecificationExecutor<OrganizationGroup> {

    @Query(
            "SELECT g FROM OrganizationGroup g WHERE g.organization.id = :organizationId AND g.archived = false")
    List<OrganizationGroup> findByOrganizationId(@Param("organizationId") Long organizationId);

    @Query("SELECT g FROM OrganizationGroup g WHERE g.organization.id = :organizationId")
    List<OrganizationGroup> findAllByOrganizationId(@Param("organizationId") Long organizationId);

    @Query(
            "SELECT g.id FROM OrganizationGroup g WHERE g.organization.id = :organizationId "
                    + "AND g.type IN :visibleTypes")
    List<Long> findIdsByOrganizationIdAndTypeIn(
            @Param("organizationId") Long organizationId,
            @Param("visibleTypes") Collection<OrganizationGroupType> visibleTypes);

    @Query(
            "SELECT g FROM OrganizationGroup g WHERE g.organization.id = :organizationId "
                    + "AND g.type = :type AND g.archived = false")
    Optional<OrganizationGroup> findByOrganizationIdAndType(
            @Param("organizationId") Long organizationId,
            @Param("type") OrganizationGroupType type);

    @Query(
            "SELECT g FROM OrganizationGroup g WHERE g.organization.id = :organizationId "
                    + "AND g.configKey = :configKey AND g.archived = false")
    Optional<OrganizationGroup> findByOrganizationIdAndConfigKey(
            @Param("organizationId") Long organizationId, @Param("configKey") String configKey);

    @Query(
            "SELECT g FROM OrganizationGroup g WHERE g.organization.id = :organizationId "
                    + "AND g.standardOriginId IS NOT NULL AND g.archived = false")
    List<OrganizationGroup> findByOrganizationIdAndStandardOriginIdIsNotNull(
            @Param("organizationId") Long organizationId);
}
