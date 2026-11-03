/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.qualification.tenant;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface TenantQualificationDrivingLicenseRepository
        extends JpaRepository<TenantQualificationDrivingLicense, Long> {

    List<TenantQualificationDrivingLicense> findByDrivingLicenseConfigKey(
            String drivingLicenseConfigKey);

    @Transactional
    void deleteByDrivingLicenseConfigKey(String drivingLicenseConfigKey);
}
