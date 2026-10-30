/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.handler;

import com.emberone.platform.security.TenantContextHolder;
import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.repository.ExitSettlementRetentionRepository;
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
public class ExitSettlementRetentionHandler implements RetentionHandler {

    private static final Set<String> TABLES = Set.of("user_contact_information", "doc_documents");

    private final ExitSettlementRetentionRepository exitSettlementRetentionRepository;
    private final RetentionObjectStorageSupport retentionObjectStorageSupport;

    @Override
    public RetentionCategory category() {
        return RetentionCategory.EXIT_SETTLEMENT;
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
        List<String> storageKeys =
                exitSettlementRetentionRepository
                        .findUnlinkedDocumentStorageKeysForDeactivatedUsersBefore(cutoff);
        for (String storageKey : storageKeys) {
            retentionObjectStorageSupport.enqueueUserDocumentObject(tenantId, storageKey);
        }
        int deleted = 0;
        deleted +=
                RetentionBatchDeletes.deleteUntilExhausted(
                        () ->
                                exitSettlementRetentionRepository
                                        .deleteUnlinkedDocumentsForDeactivatedUsersBefore(cutoff));
        deleted +=
                RetentionBatchDeletes.deleteUntilExhausted(
                        () ->
                                exitSettlementRetentionRepository
                                        .deleteContactInformationForDeactivatedUsersBefore(cutoff));
        return deleted;
    }
}
