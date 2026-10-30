/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.drivinglicense.repository;

import com.emberone.drivinglicense.domain.UserDrivingLicense;
import com.emberone.platform.navigation.domain.AppLinkType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface UserDrivingLicenseRepository extends JpaRepository<UserDrivingLicense, Long> {

    Optional<UserDrivingLicense> findByUserId(Long userId);

    Optional<UserDrivingLicense> findByAppLinkTypeAndAppLinkId(
            AppLinkType appLinkType, Long appLinkId);

    @Query(
            """
        SELECT CASE WHEN COUNT(licenseClass) > 0 THEN true ELSE false END
        FROM UserDrivingLicenseClass licenseClass
        WHERE licenseClass.tenantDrivingLicenseDefinitionId = :definitionId
        """)
    boolean existsByClasses_TenantDrivingLicenseDefinitionId(
            @Param("definitionId") Long definitionId);
}
