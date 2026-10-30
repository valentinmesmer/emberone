/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.service;

import com.emberone.certification.domain.UserCertificationRecordDocument;
import com.emberone.certification.repository.UserCertificationRecordDocumentRepository;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.retention.repository.CertificationRecordRetentionRepository;
import com.emberone.userdocument.service.UserDocumentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes certification records and junction-linked documents (not {@code CERTIFICATION_RECORD}
 * link targets).
 */
@Service
@RequiredArgsConstructor
public class CertificationRecordRetentionSupport {

    private static final int CHUNK_SIZE = 500;

    private final CertificationRecordRetentionRepository certificationRecordRetentionRepository;
    private final UserCertificationRecordDocumentRepository recordDocumentRepository;
    private final UserDocumentService userDocumentService;

    /**
     * Documents still linked to a record outside the deleted set are kept; a document shared only
     * by deleted records is removed once.
     */
    @Transactional
    public int deleteRecordsByIds(List<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return 0;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is required for retention");
        }
        int deleted = 0;
        for (int start = 0; start < recordIds.size(); start += CHUNK_SIZE) {
            List<Long> chunk =
                    recordIds.subList(start, Math.min(start + CHUNK_SIZE, recordIds.size()));
            List<Long> documentIds =
                    recordDocumentRepository.findByRecord_IdIn(chunk).stream()
                            .map(UserCertificationRecordDocument::getDocumentId)
                            .distinct()
                            .filter(
                                    documentId ->
                                            !recordDocumentRepository
                                                    .existsByDocumentIdAndRecord_IdNotIn(
                                                            documentId, chunk))
                            .toList();
            userDocumentService.deleteDocumentsByIds(tenantId, documentIds);
            deleted += certificationRecordRetentionRepository.deleteRecordsByIds(chunk);
        }
        return deleted;
    }
}
