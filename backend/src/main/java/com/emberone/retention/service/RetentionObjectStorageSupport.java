/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.service;

import com.emberone.platform.storage.service.ObjectStorageDeletionOutboxService;
import com.emberone.platform.storage.service.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RetentionObjectStorageSupport {

    private final ObjectStorageDeletionOutboxService deletionOutboxService;
    private final ObjectStorageService objectStorageService;

    @Value("${object-storage.user-documents-bucket}")
    private String userDocumentsBucket;

    @Value("${object-storage.workflows-bucket}")
    private String workflowsBucket;

    @Value("${object-storage.attachments-bucket}")
    private String attachmentsBucket;

    public void enqueueUserStoragePrefix(Long tenantId, Long userId) {
        String prefix = "tenants/" + tenantId + "/users/" + userId + "/";
        deletionOutboxService.enqueuePrefixDeletion(tenantId, userDocumentsBucket, prefix);
        deletionOutboxService.enqueuePrefixDeletion(
                tenantId, objectStorageService.profileImagesBucket(), prefix);
    }

    public void enqueueUserDocumentObject(Long tenantId, String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            return;
        }
        deletionOutboxService.enqueueObjectDeletion(tenantId, userDocumentsBucket, storageKey);
    }

    public void enqueueProfileImageObject(Long tenantId, String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return;
        }
        deletionOutboxService.enqueueObjectDeletion(
                tenantId, objectStorageService.profileImagesBucket(), objectKey);
    }

    public void enqueueWorkflowObject(Long tenantId, String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return;
        }
        deletionOutboxService.enqueueObjectDeletion(tenantId, workflowsBucket, objectKey);
    }

    public void enqueueAttachmentObject(Long tenantId, String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return;
        }
        deletionOutboxService.enqueueObjectDeletion(tenantId, attachmentsBucket, objectKey);
    }
}
