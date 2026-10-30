/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permissiongroup.repository;

import com.emberone.permission.domain.PermissionGroupType;
import com.emberone.permissiongroup.domain.PermissionGroup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface PermissionGroupRepository extends JpaRepository<PermissionGroup, Long> {

    Optional<PermissionGroup> findByOrganizationIdAndType(
            Long organizationId, PermissionGroupType type);

    Optional<PermissionGroup> findByOrganizationIdAndConfigKey(
            Long organizationId, String configKey);

    List<PermissionGroup> findByOrganizationIdAndStandardOriginIdIsNotNull(Long organizationId);

    @Query(
            "SELECT DISTINCT pg FROM PermissionGroup pg "
                    + "LEFT JOIN FETCH pg.permissionTypeLinks "
                    + "WHERE pg.organizationId = :organizationId")
    List<PermissionGroup> findAllByOrganizationId(@Param("organizationId") Long organizationId);
}
