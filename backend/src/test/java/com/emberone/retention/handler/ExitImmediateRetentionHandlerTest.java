/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.platform.security.TenantContextHolder;
import com.emberone.retention.repository.ExitImmediateRetentionRepository;
import com.emberone.retention.service.RetentionObjectStorageSupport;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExitImmediateRetentionHandlerTest {

    @Mock ExitImmediateRetentionRepository exitImmediateRetentionRepository;
    @Mock RetentionObjectStorageSupport retentionObjectStorageSupport;

    @InjectMocks ExitImmediateRetentionHandler handler;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(3L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void enqueuesProfileImageObjectsBeforeDeletingRows() {
        Instant cutoff = Instant.parse("2024-01-01T00:00:00Z");
        when(exitImmediateRetentionRepository.findProfileImageObjectKeysForDeactivatedUsersBefore(
                        cutoff))
                .thenReturn(List.of("tenants/3/users/9/profile-image/a.png"));
        when(exitImmediateRetentionRepository.deleteProfileImagesForDeactivatedUsersBefore(cutoff))
                .thenReturn(1, 0);
        when(exitImmediateRetentionRepository.deleteEmergencyContactsForDeactivatedUsersBefore(
                        cutoff))
                .thenReturn(2, 0);
        when(exitImmediateRetentionRepository.deleteChannelOverridesForDeactivatedUsersBefore(
                        cutoff))
                .thenReturn(0);

        assertThat(handler.apply(cutoff)).isEqualTo(3);
        verify(retentionObjectStorageSupport)
                .enqueueProfileImageObject(3L, "tenants/3/users/9/profile-image/a.png");
    }
}
