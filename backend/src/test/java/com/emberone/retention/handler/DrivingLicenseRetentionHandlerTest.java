/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.retention.repository.DrivingLicenseRetentionRepository;
import com.emberone.userdocument.service.UserDocumentService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DrivingLicenseRetentionHandlerTest {

    @Mock DrivingLicenseRetentionRepository drivingLicenseRetentionRepository;
    @Mock UserDocumentService userDocumentService;

    @InjectMocks DrivingLicenseRetentionHandler handler;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void deletesDocumentsBeforePurgingLicenseRows() {
        TenantContextHolder.setTenantId(9L);
        Instant cutoff = Instant.parse("2024-01-01T00:00:00Z");
        when(drivingLicenseRetentionRepository.findDrivingLicenseIdsForDeactivatedUsersBefore(
                        cutoff))
                .thenReturn(List.of(42L));
        when(userDocumentService.deleteDocumentsForLinkTargets(
                        9L, AppLinkType.DRIVING_LICENSE_RECORD, List.of(42L)))
                .thenReturn(1);
        when(drivingLicenseRetentionRepository.deleteDrivingLicensesForDeactivatedUsersBefore(
                        cutoff))
                .thenReturn(1, 0);

        assertThat(handler.apply(cutoff)).isEqualTo(2);

        verify(userDocumentService)
                .deleteDocumentsForLinkTargets(
                        9L, AppLinkType.DRIVING_LICENSE_RECORD, List.of(42L));
        verify(drivingLicenseRetentionRepository, times(2))
                .deleteDrivingLicensesForDeactivatedUsersBefore(cutoff);
    }
}
