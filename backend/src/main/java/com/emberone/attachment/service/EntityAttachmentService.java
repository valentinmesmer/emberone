/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.attachment.service;

import com.emberone.attachment.domain.EntityAttachment;
import com.emberone.attachment.domain.EntityAttachmentType;
import com.emberone.attachment.dto.EntityAttachmentDto;
import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.platform.security.EmberOneJwtClaims;
import com.emberone.platform.storage.service.ObjectStorageService;
import com.emberone.platform.storage.service.ObjectStorageTransactionCoordinator;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityAttachmentService {

    public static final long MAX_FILE_SIZE = 50 * 1024 * 1024L;

    private final EntityAttachmentRepository entityAttachmentRepository;
    private final ObjectStorageService objectStorageService;
    private final ObjectStorageTransactionCoordinator objectStorageTransactionCoordinator;

    public static void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size must be less than 50MB");
        }
    }

    public static void validateContentType(MultipartFile file, Set<String> allowedMediaTypes) {
        String contentType = file.getContentType();
        if (contentType == null
                || contentType.isBlank()
                || !allowedMediaTypes.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Unsupported file type; allowed: PDF, JPEG, PNG, GIF, WebP");
        }
    }

    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }

    private static EntityAttachment buildAttachment(
            Long tenantId,
            Long userId,
            Long organizationId,
            EntityAttachmentType entityType,
            Long entityId,
            MultipartFile file,
            String storageKey) {
        return EntityAttachment.builder()
                .tenantId(tenantId)
                .entityType(entityType)
                .entityId(entityId)
                .organizationId(organizationId)
                .fileName(
                        file.getOriginalFilename() != null
                                ? file.getOriginalFilename()
                                : "attachment")
                .contentType(
                        file.getContentType() != null
                                ? file.getContentType()
                                : "application/octet-stream")
                .sizeBytes(file.getSize())
                .storageKey(storageKey)
                .uploadedBy(EmberOneJwtClaims.referencedUserId(userId))
                .build();
    }

    @Transactional(readOnly = true)
    public List<EntityAttachmentDto> listByEntity(EntityAttachmentType entityType, Long entityId) {
        return sortNewestFirst(
                        entityAttachmentRepository.findByEntityTypeAndEntityId(
                                entityType, entityId))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EntityAttachmentDto> listByEntity(
            EntityAttachmentType entityType, Long entityId, Long organizationId) {
        return sortNewestFirst(
                        entityAttachmentRepository.findByEntityTypeAndEntityIdAndOrganizationId(
                                entityType, entityId, organizationId))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EntityAttachment> findEntitiesByTypeAndId(
            EntityAttachmentType entityType, Long entityId) {
        return sortNewestFirst(
                entityAttachmentRepository.findByEntityTypeAndEntityId(entityType, entityId));
    }

    @Transactional
    public EntityAttachment uploadNew(
            String bucket,
            Long tenantId,
            Long userId,
            Long organizationId,
            EntityAttachmentType entityType,
            Long entityId,
            MultipartFile file,
            String storageKey) {
        validateFile(file);
        objectStorageTransactionCoordinator.uploadFileWithRollbackCleanup(file, bucket, storageKey);
        EntityAttachment saved =
                entityAttachmentRepository.save(
                        buildAttachment(
                                tenantId,
                                userId,
                                organizationId,
                                entityType,
                                entityId,
                                file,
                                storageKey));
        log.info("Uploaded attachment {} ({}) for entity {}", saved.getId(), entityType, entityId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<EntityAttachment> findScoped(
            Long attachmentId,
            EntityAttachmentType entityType,
            Long entityId,
            Long organizationId) {
        return entityAttachmentRepository.findByIdAndEntityTypeAndEntityIdAndOrganizationId(
                attachmentId, entityType, entityId, organizationId);
    }

    @Transactional(readOnly = true)
    public EntityAttachment requireScoped(
            Long attachmentId,
            EntityAttachmentType entityType,
            Long entityId,
            Long organizationId) {
        return findScoped(attachmentId, entityType, entityId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));
    }

    @Transactional(readOnly = true)
    public InputStreamResource download(String bucket, EntityAttachment attachment) {
        return new InputStreamResource(
                objectStorageService.downloadFile(bucket, attachment.getStorageKey()));
    }

    @Transactional
    public void delete(String bucket, EntityAttachment attachment) {
        String storageKey = attachment.getStorageKey();
        entityAttachmentRepository.delete(attachment);
        objectStorageTransactionCoordinator.deleteFileAfterCommit(bucket, storageKey);
        log.info(
                "Deleted attachment {} ({}) for entity {}",
                attachment.getId(),
                attachment.getEntityType(),
                attachment.getEntityId());
    }

    @Transactional
    public EntityAttachment relink(
            String bucket,
            EntityAttachment attachment,
            String newStorageKey,
            EntityAttachmentType entityType,
            Long entityId) {
        String previousStorageKey = attachment.getStorageKey();
        objectStorageTransactionCoordinator.copyFileWithRollbackCleanup(
                bucket, previousStorageKey, bucket, newStorageKey);
        attachment.setStorageKey(newStorageKey);
        attachment.setEntityType(entityType);
        attachment.setEntityId(entityId);
        EntityAttachment saved = entityAttachmentRepository.save(attachment);
        objectStorageTransactionCoordinator.deleteFileAfterCommit(bucket, previousStorageKey);
        log.info("Relinked attachment {} to {} entity {}", saved.getId(), entityType, entityId);
        return saved;
    }

    @Transactional
    public EntityAttachment move(String bucket, EntityAttachment attachment, String newStorageKey) {
        String previousStorageKey = attachment.getStorageKey();
        objectStorageTransactionCoordinator.copyFileWithRollbackCleanup(
                bucket, previousStorageKey, bucket, newStorageKey);
        attachment.setStorageKey(newStorageKey);
        EntityAttachment saved = entityAttachmentRepository.save(attachment);
        objectStorageTransactionCoordinator.deleteFileAfterCommit(bucket, previousStorageKey);
        log.info("Moved attachment {} in object storage", saved.getId());
        return saved;
    }

    @Transactional
    public EntityAttachment save(EntityAttachment attachment) {
        return entityAttachmentRepository.save(attachment);
    }

    public EntityAttachmentDto toDto(EntityAttachment attachment) {
        return EntityAttachmentDto.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .contentType(attachment.getContentType())
                .sizeBytes(attachment.getSizeBytes())
                .uploadedBy(attachment.getUploadedBy())
                .build();
    }

    public List<EntityAttachment> sortNewestFirst(List<EntityAttachment> attachments) {
        return attachments.stream()
                .sorted(Comparator.comparing(EntityAttachment::getId, Comparator.reverseOrder()))
                .toList();
    }

    public DownloadBundle downloadBundle(String bucket, EntityAttachment attachment) {
        return new DownloadBundle(
                download(bucket, attachment),
                attachment.getFileName(),
                attachment.getContentType());
    }

    public record DownloadBundle(
            InputStreamResource resource, String fileName, String contentType) {}
}
