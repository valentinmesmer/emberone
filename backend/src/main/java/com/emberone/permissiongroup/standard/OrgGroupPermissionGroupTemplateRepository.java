/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permissiongroup.standard;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface OrgGroupPermissionGroupTemplateRepository
        extends JpaRepository<OrgGroupPermissionGroupTemplate, Long> {

    List<OrgGroupPermissionGroupTemplate> findByStandardId(Long standardId);

    List<OrgGroupPermissionGroupTemplate> findByStandardIdAndOrgGroupConfigKey(
            Long standardId, String orgGroupConfigKey);

    List<OrgGroupPermissionGroupTemplate> findByStandardIdAndPermissionGroupConfigKey(
            Long standardId, String permissionGroupConfigKey);

    Optional<OrgGroupPermissionGroupTemplate>
            findByStandardIdAndOrgGroupConfigKeyAndPermissionGroupConfigKey(
                    Long standardId, String orgGroupConfigKey, String permissionGroupConfigKey);

    boolean existsByStandardIdAndOrgGroupConfigKeyAndPermissionGroupConfigKey(
            Long standardId, String orgGroupConfigKey, String permissionGroupConfigKey);
}
