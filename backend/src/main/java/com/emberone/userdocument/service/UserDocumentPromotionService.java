/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.service;

import com.emberone.attachment.service.EntityAttachmentService;
import com.emberone.platform.storage.service.ObjectStorageTransactionCoordinator;
import com.emberone.userdocument.domain.UserDocument;
import com.emberone.userdocument.dto.UserDocumentLinkInput;
import com.emberone.userdocument.repository.UserDocumentRepository;
import com.emberone.userdocument.util.UserDocumentFileNames;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Copies attachments from object storage into a user's document folder during trusted system
 * promotions (onboarding, workflows).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDocumentPromotionService {

    private final ObjectStorageTransactionCoordinator objectStorageTransactionCoordinator;
    private final UserDocumentRepository userDocumentRepository;
    private final UserDocumentSystemWriter userDocumentSystemWriter;

    @Value("${object-storage.user-documents-bucket}")
    private String userDocumentsBucket;

    public Long copyFromStorage(
            long tenantId,
            long userId,
            String sourceBucket,
            String sourceKey,
            String fileName,
            String contentType,
            Long sizeBytes,
            String title,
            String description,
            List<String> tags,
            List<UserDocumentLinkInput> links) {
        if (!StringUtils.hasText(sourceKey)) {
            return null;
        }
        String extension = EntityAttachmentService.getFileExtension(fileName);
        String destinationKey =
                "tenants/"
                        + tenantId
                        + "/users/"
                        + userId
                        + "/documents/"
                        + UUID.randomUUID()
                        + extension;
        objectStorageTransactionCoordinator.copyFileWithRollbackCleanup(
                sourceBucket, sourceKey, userDocumentsBucket, destinationKey);

        String resolvedFileName =
                UserDocumentFileNames.displayFileName(
                        title, extension, StringUtils.hasText(fileName) ? fileName : "document");
        String resolvedContentType =
                StringUtils.hasText(contentType) ? contentType : "application/octet-stream";

        UserDocument document =
                UserDocument.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .title(StringUtils.hasText(title) ? title.trim() : null)
                        .description(StringUtils.hasText(description) ? description.trim() : null)
                        .fileName(resolvedFileName)
                        .contentType(resolvedContentType)
                        .sizeBytes(sizeBytes != null ? sizeBytes : 0L)
                        .storageKey(destinationKey)
                        .build();

        userDocumentSystemWriter.applyTags(document, tags != null ? tags : List.of());
        userDocumentSystemWriter.applyTrustedLinks(document, links);

        Long documentId = userDocumentRepository.save(document).getId();
        log.info("Promoted user document {} for user {}", documentId, userId);
        return documentId;
    }
}
