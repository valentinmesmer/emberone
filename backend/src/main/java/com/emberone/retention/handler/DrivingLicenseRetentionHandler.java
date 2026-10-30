/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.handler;

import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.repository.DrivingLicenseRetentionRepository;
import com.emberone.retention.spi.RetentionHandler;
import com.emberone.retention.support.RetentionBatchDeletes;
import com.emberone.userdocument.service.UserDocumentService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrivingLicenseRetentionHandler implements RetentionHandler {

    private static final Set<String> TABLES =
            Set.of("driving_licenses", "driving_license_classes", "driving_license_documents");

    private final DrivingLicenseRetentionRepository drivingLicenseRetentionRepository;
    private final UserDocumentService userDocumentService;

    @Override
    public RetentionCategory category() {
        return RetentionCategory.DRIVING_LICENSE;
    }

    @Override
    public Set<String> tables() {
        return TABLES;
    }

    @Override
    public int apply(Instant cutoff) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is required for retention");
        }
        List<Long> licenseIds =
                drivingLicenseRetentionRepository.findDrivingLicenseIdsForDeactivatedUsersBefore(
                        cutoff);
        int deleted =
                userDocumentService.deleteDocumentsForLinkTargets(
                        tenantId, AppLinkType.DRIVING_LICENSE_RECORD, licenseIds);
        deleted +=
                RetentionBatchDeletes.deleteUntilExhausted(
                        () ->
                                drivingLicenseRetentionRepository
                                        .deleteDrivingLicensesForDeactivatedUsersBefore(cutoff));
        return deleted;
    }
}
