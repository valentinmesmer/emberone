/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emberone.certification.domain.UserCertificationRecord;
import com.emberone.certification.domain.UserCertificationRecordDocument;
import com.emberone.certification.repository.UserCertificationRecordDocumentRepository;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.retention.repository.CertificationRecordRetentionRepository;
import com.emberone.userdocument.service.UserDocumentService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CertificationRecordRetentionSupportTest {

    private static final long TENANT_ID = 4L;

    @Mock CertificationRecordRetentionRepository certificationRecordRetentionRepository;
    @Mock UserCertificationRecordDocumentRepository recordDocumentRepository;
    @Mock UserDocumentService userDocumentService;

    @InjectMocks CertificationRecordRetentionSupport support;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void deletesJunctionDocumentsByIdKeepingDocumentsOfRetainedRecords() {
        List<Long> recordIds = List.of(1L, 2L);
        when(recordDocumentRepository.findByRecord_IdIn(recordIds))
                .thenReturn(List.of(link(1L, 100L), link(2L, 100L), link(2L, 200L)));
        when(recordDocumentRepository.existsByDocumentIdAndRecord_IdNotIn(100L, recordIds))
                .thenReturn(false);
        when(recordDocumentRepository.existsByDocumentIdAndRecord_IdNotIn(200L, recordIds))
                .thenReturn(true);
        when(certificationRecordRetentionRepository.deleteRecordsByIds(recordIds)).thenReturn(2);

        assertThat(support.deleteRecordsByIds(recordIds)).isEqualTo(2);

        verify(userDocumentService).deleteDocumentsByIds(TENANT_ID, List.of(100L));
        verify(userDocumentService, never()).deleteDocumentsForLinkTargets(any(), any(), any());
    }

    @Test
    void noRecordsIsNoOp() {
        assertThat(support.deleteRecordsByIds(List.of())).isZero();

        verifyNoInteractions(
                recordDocumentRepository,
                userDocumentService,
                certificationRecordRetentionRepository);
    }

    private static UserCertificationRecordDocument link(long recordId, long documentId) {
        return UserCertificationRecordDocument.builder()
                .tenantId(TENANT_ID)
                .record(UserCertificationRecord.builder().id(recordId).build())
                .documentId(documentId)
                .build();
    }
}
