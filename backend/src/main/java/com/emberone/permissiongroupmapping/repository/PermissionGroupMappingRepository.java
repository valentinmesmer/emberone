/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permissiongroupmapping.repository;

import com.emberone.permissiongroupmapping.domain.PermissionGroupMapping;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface PermissionGroupMappingRepository
        extends JpaRepository<PermissionGroupMapping, Long> {

    List<PermissionGroupMapping> findByOrganizationGroupId(Long organizationGroupId);

    List<PermissionGroupMapping> findByOrganizationGroupIdIn(Set<Long> organizationGroupIds);

    List<PermissionGroupMapping> findByPermissionGroup_Id(Long permissionGroupId);

    boolean existsByOrganizationGroupIdAndPermissionGroup_Id(
            Long organizationGroupId, Long permissionGroupId);

    @Transactional
    void deleteByOrganizationGroupId(Long organizationGroupId);

    /**
     * Mappings of the organization's active groups to a profile of the same organization that
     * grants the permission type.
     */
    /**
     * Organizations of the tenant whose active groups map to a profile of the same organization
     * that grants the permission type. One query for the whole tenant, used to prefilter lists.
     */
    @Query(
            "SELECT DISTINCT pg.organizationId FROM PermissionGroupMapping m "
                    + "JOIN m.permissionGroup pg JOIN pg.permissionTypeLinks link "
                    + "WHERE link.permissionType = :permissionTypeId "
                    + "AND m.organizationGroupId IN (SELECT g.id FROM OrganizationGroup g "
                    + "WHERE g.organization.id = pg.organizationId AND g.archived = false)")
    List<Long> findOrganizationIdsGrantingPermission(
            @Param("permissionTypeId") Long permissionTypeId);

    @Query(
            "SELECT COUNT(m) FROM PermissionGroupMapping m "
                    + "JOIN m.permissionGroup pg JOIN pg.permissionTypeLinks link "
                    + "WHERE pg.organizationId = :organizationId "
                    + "AND link.permissionType = :permissionTypeId "
                    + "AND m.organizationGroupId IN (SELECT g.id FROM OrganizationGroup g "
                    + "WHERE g.organization.id = :organizationId AND g.archived = false)")
    long countMappingsGrantingPermission(
            @Param("organizationId") Long organizationId,
            @Param("permissionTypeId") Long permissionTypeId);

    @Transactional
    void deleteByPermissionGroupId(Long permissionGroupId);
}
