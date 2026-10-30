/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.retention.repository.WorkflowRetentionRepository;
import com.emberone.retention.service.RetentionObjectStorageSupport;
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
class WorkflowRetentionHandlerTest {

    @Mock WorkflowRetentionRepository workflowRetentionRepository;
    @Mock RetentionObjectStorageSupport retentionObjectStorageSupport;
    @Mock UserDocumentService userDocumentService;

    @InjectMocks WorkflowRetentionHandler handler;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void enqueuesWorkflowBucketDeletesBeforePurgingRows() {
        TenantContextHolder.setTenantId(7L);
        Instant cutoff = Instant.parse("2024-01-01T00:00:00Z");
        when(workflowRetentionRepository.findAttachmentStorageKeysForWorkflowsDecidedBefore(cutoff))
                .thenReturn(List.of("tenants/7/workflows/5/doc.pdf"));
        when(workflowRetentionRepository.deleteWorkflowCommentAttachmentsForWorkflowsDecidedBefore(
                        cutoff))
                .thenReturn(0);
        when(workflowRetentionRepository.deleteWorkflowAttachmentsForWorkflowsDecidedBefore(cutoff))
                .thenReturn(1, 0);
        when(workflowRetentionRepository.deleteWorkflowsDecidedBefore(cutoff)).thenReturn(1, 0);

        handler.apply(cutoff);

        verify(retentionObjectStorageSupport)
                .enqueueWorkflowObject(7L, "tenants/7/workflows/5/doc.pdf");
    }

    @Test
    void deletesDocumentsLinkedToExpiredWorkflows() {
        TenantContextHolder.setTenantId(7L);
        Instant cutoff = Instant.parse("2024-01-01T00:00:00Z");
        when(workflowRetentionRepository.findWorkflowIdsDecidedBefore(cutoff))
                .thenReturn(List.of(5L));
        when(userDocumentService.deleteDocumentsForLinkTargets(
                        7L, AppLinkType.WORKFLOW, List.of(5L)))
                .thenReturn(2);
        when(workflowRetentionRepository.deleteWorkflowCommentAttachmentsForWorkflowsDecidedBefore(
                        cutoff))
                .thenReturn(0);
        when(workflowRetentionRepository.deleteWorkflowAttachmentsForWorkflowsDecidedBefore(cutoff))
                .thenReturn(0);
        when(workflowRetentionRepository.deleteWorkflowsDecidedBefore(cutoff)).thenReturn(1, 0);

        assertThat(handler.apply(cutoff)).isEqualTo(3);
    }
}
