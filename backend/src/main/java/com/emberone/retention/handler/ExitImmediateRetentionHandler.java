/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.handler;

import com.emberone.platform.security.TenantContextHolder;
import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.repository.ExitImmediateRetentionRepository;
import com.emberone.retention.service.RetentionObjectStorageSupport;
import com.emberone.retention.spi.RetentionHandler;
import com.emberone.retention.support.RetentionBatchDeletes;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExitImmediateRetentionHandler implements RetentionHandler {

    private static final Set<String> TABLES =
            Set.of(
                    "user_emergency_contacts",
                    "doc_user_profile_images",
                    "ntf_user_channel_overrides");

    private final ExitImmediateRetentionRepository exitImmediateRetentionRepository;
    private final RetentionObjectStorageSupport retentionObjectStorageSupport;

    @Override
    public RetentionCategory category() {
        return RetentionCategory.EXIT_IMMEDIATE;
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
        List<String> profileImageKeys =
                exitImmediateRetentionRepository
                        .findProfileImageObjectKeysForDeactivatedUsersBefore(cutoff);
        for (String objectKey : profileImageKeys) {
            retentionObjectStorageSupport.enqueueProfileImageObject(tenantId, objectKey);
        }
        int deleted = 0;
        deleted +=
                RetentionBatchDeletes.deleteUntilExhausted(
                        () ->
                                exitImmediateRetentionRepository
                                        .deleteProfileImagesForDeactivatedUsersBefore(cutoff));
        deleted +=
                RetentionBatchDeletes.deleteUntilExhausted(
                        () ->
                                exitImmediateRetentionRepository
                                        .deleteEmergencyContactsForDeactivatedUsersBefore(cutoff));
        deleted +=
                RetentionBatchDeletes.deleteUntilExhausted(
                        () ->
                                exitImmediateRetentionRepository
                                        .deleteChannelOverridesForDeactivatedUsersBefore(cutoff));
        return deleted;
    }
}
