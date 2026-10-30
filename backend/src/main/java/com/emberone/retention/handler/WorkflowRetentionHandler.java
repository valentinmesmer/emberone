/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.handler;

import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.repository.WorkflowRetentionRepository;
import com.emberone.retention.service.RetentionObjectStorageSupport;
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
public class WorkflowRetentionHandler implements RetentionHandler {

    private static final Set<String> TABLES =
            Set.of(
                    "wf_instances",
                    "wf_comments",
                    "wf_decisions",
                    "wf_input_changes",
                    "wf_participants",
                    "wf_exclusivity_locks");

    private final WorkflowRetentionRepository workflowRetentionRepository;
    private final RetentionObjectStorageSupport retentionObjectStorageSupport;
    private final UserDocumentService userDocumentService;

    @Override
    public RetentionCategory category() {
        return RetentionCategory.WORKFLOW;
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
                workflowRetentionRepository.findAttachmentStorageKeysForWorkflowsDecidedBefore(
                        cutoff);
        for (String storageKey : storageKeys) {
            retentionObjectStorageSupport.enqueueWorkflowObject(tenantId, storageKey);
        }
        int deleted =
                userDocumentService.deleteDocumentsForLinkTargets(
                        tenantId,
                        AppLinkType.WORKFLOW,
                        workflowRetentionRepository.findWorkflowIdsDecidedBefore(cutoff));
        deleted +=
                RetentionBatchDeletes.deleteUntilExhausted(
                        () ->
                                workflowRetentionRepository
                                        .deleteWorkflowCommentAttachmentsForWorkflowsDecidedBefore(
                                                cutoff));
        deleted +=
                RetentionBatchDeletes.deleteUntilExhausted(
                        () ->
                                workflowRetentionRepository
                                        .deleteWorkflowAttachmentsForWorkflowsDecidedBefore(
                                                cutoff));
        deleted +=
                RetentionBatchDeletes.deleteUntilExhausted(
                        () -> workflowRetentionRepository.deleteWorkflowsDecidedBefore(cutoff));
        return deleted;
    }
}
