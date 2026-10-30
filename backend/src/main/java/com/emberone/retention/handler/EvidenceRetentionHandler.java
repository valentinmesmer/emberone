/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.handler;

import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.repository.CertificationRecordRetentionRepository;
import com.emberone.retention.service.CertificationRecordRetentionSupport;
import com.emberone.retention.spi.RetentionHandler;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Deletes {@code cert_user_records} with an {@code expiry_date} once {@code expiry_date + profile
 * period} has passed, including linked scans.
 */
@Component
@RequiredArgsConstructor
public class EvidenceRetentionHandler implements RetentionHandler {

    private static final Set<String> TABLES =
            Set.of("cert_user_records", "cert_user_record_documents");

    private final CertificationRecordRetentionRepository certificationRecordRetentionRepository;
    private final CertificationRecordRetentionSupport certificationRecordRetentionSupport;

    @Override
    public RetentionCategory category() {
        return RetentionCategory.EVIDENCE;
    }

    @Override
    public Set<String> tables() {
        return TABLES;
    }

    @Override
    public int apply(Instant cutoff) {
        List<Long> recordIds =
                certificationRecordRetentionRepository.findEvidenceRecordIdsExpiredBefore(cutoff);
        return certificationRecordRetentionSupport.deleteRecordsByIds(recordIds);
    }
}
