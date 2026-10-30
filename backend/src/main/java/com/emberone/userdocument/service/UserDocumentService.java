/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.service;

import com.emberone.attachment.AllowedAttachmentMediaTypes;
import com.emberone.attachment.service.EntityAttachmentService;
import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.storage.service.ObjectStorageDeletionOutboxService;
import com.emberone.platform.storage.service.ObjectStorageService;
import com.emberone.platform.storage.service.ObjectStorageTransactionCoordinator;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.PageReorder;
import com.emberone.user.domain.User;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.repository.UserRepository;
import com.emberone.userdocument.domain.UserDocument;
import com.emberone.userdocument.domain.UserDocumentLink;
import com.emberone.userdocument.domain.UserDocumentTag;
import com.emberone.userdocument.dto.UpdateUserDocumentRequest;
import com.emberone.userdocument.dto.UserDocumentDto;
import com.emberone.userdocument.dto.UserDocumentLinkDto;
import com.emberone.userdocument.dto.UserDocumentPageDto;
import com.emberone.userdocument.list.UserDocumentListDefinition;
import com.emberone.userdocument.list.UserDocumentListFilter;
import com.emberone.userdocument.list.UserDocumentListSpecs;
import com.emberone.userdocument.repository.UserDocumentRepository;
import com.emberone.userdocument.util.UserDocumentFileNames;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDocumentService {

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024L;

    private static final Set<AppLinkType> RECORD_LINK_TYPES =
            EnumSet.of(
                    AppLinkType.CERTIFICATION_RECORD,
                    AppLinkType.TRAINING_COURSE_RECORD,
                    AppLinkType.DRIVING_LICENSE_RECORD);

    private static final int DELETE_CHUNK_SIZE = 1000;

    private final UserDocumentRepository userDocumentRepository;
    private final UserDocumentLinkResolver linkResolver;
    private final UserDocumentSystemWriter systemWriter;
    private final ObjectStorageService objectStorageService;
    private final ObjectStorageTransactionCoordinator objectStorageTransactionCoordinator;
    private final ObjectStorageDeletionOutboxService deletionOutboxService;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final UserOrganizationMembershipRepository userOrganizationMembershipRepository;

    @Value("${object-storage.user-documents-bucket}")
    private String userDocumentsBucket;

    @Transactional(readOnly = true)
    public UserDocumentPageDto listDocuments(
            Long userId, ListQuery query, UserDocumentListFilter filter) {
        Pageable pageable = UserDocumentListDefinition.INSTANCE.pageable(query);
        Specification<UserDocument> spec =
                Specification.allOf(
                        UserDocumentListSpecs.forUser(userId),
                        UserDocumentListSpecs.tagFilter(filter.tag()),
                        UserDocumentListSpecs.viewedFilter(filter.viewed()),
                        UserDocumentListSpecs.contentTypeFilter(filter.contentType()),
                        UserDocumentListDefinition.INSTANCE.searchSpecification(query));
        Page<UserDocument> entityPage = userDocumentRepository.findAll(spec, pageable);
        List<Long> ids = entityPage.getContent().stream().map(UserDocument::getId).toList();
        List<UserDocument> hydrated =
                ids.isEmpty() ? List.of() : userDocumentRepository.findWithDetailsByIdIn(ids);
        List<UserDocument> ordered = PageReorder.reorderByIds(ids, hydrated, UserDocument::getId);
        Page<UserDocument> page = PageReorder.pageOf(ordered, entityPage);
        return toPageDto(page);
    }

    @Transactional(readOnly = true)
    public List<String> listDocumentTags(Long userId) {
        return userDocumentRepository.findDistinctTagsForUser(userId).stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> listDocumentContentTypes(Long userId) {
        return userDocumentRepository.findDistinctContentTypesForUser(userId).stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return userDocumentRepository.countByUserIdAndViewedAtIsNull(userId);
    }

    @Transactional
    public void markViewed(Long userId, Long documentId) {
        UserDocument document = findOwnedDocument(userId, documentId);
        if (document.getViewedAt() == null) {
            document.setViewedAt(Instant.now());
            userDocumentRepository.save(document);
        }
    }

    @Transactional
    public void markAllViewed(Long userId) {
        userDocumentRepository.markAllViewedForUser(userId, Instant.now());
    }

    @Transactional
    public UserDocumentDto uploadDocument(
            Long userId, MultipartFile file, String title, String description, List<String> tags) {
        validateFile(file);

        Long tenantId = requireTenantId();
        String normalizedTitle = StringUtils.hasText(title) ? title.trim() : null;
        String extension = EntityAttachmentService.getFileExtension(file.getOriginalFilename());
        String storageKey =
                "tenants/"
                        + tenantId
                        + "/users/"
                        + userId
                        + "/documents/"
                        + UUID.randomUUID()
                        + extension;

        objectStorageTransactionCoordinator.uploadFileWithRollbackCleanup(
                file, userDocumentsBucket, storageKey);

        String fallbackFileName =
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String fileName =
                UserDocumentFileNames.displayFileName(normalizedTitle, extension, fallbackFileName);
        String contentType =
                file.getContentType() != null
                        ? file.getContentType().toLowerCase()
                        : "application/octet-stream";
        UserDocument document =
                UserDocument.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .title(normalizedTitle)
                        .description(StringUtils.hasText(description) ? description.trim() : null)
                        .fileName(fileName)
                        .contentType(contentType)
                        .sizeBytes(file.getSize())
                        .storageKey(storageKey)
                        .build();

        systemWriter.applyTags(document, tags);

        UserDocument saved = userDocumentRepository.save(document);
        log.info("Uploaded user document {} for user {}", saved.getId(), userId);
        return toDto(saved);
    }

    @Transactional
    public UserDocumentDto updateDocument(
            Long userId, Long documentId, UpdateUserDocumentRequest request) {
        UserDocument document = findOwnedDocument(userId, documentId);

        if (request.getTitle() != null) {
            document.setTitle(
                    StringUtils.hasText(request.getTitle()) ? request.getTitle().trim() : null);
        }
        if (request.getDescription() != null) {
            document.setDescription(
                    StringUtils.hasText(request.getDescription())
                            ? request.getDescription().trim()
                            : null);
        }
        if (request.getTags() != null) {
            document.getTags().clear();
            systemWriter.applyTags(document, request.getTags());
        }

        UserDocument saved = userDocumentRepository.save(document);
        log.info("Updated user document {} for user {}", documentId, userId);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public DocumentDownload downloadDocument(Long userId, Long documentId) {
        UserDocument document = findOwnedDocument(userId, documentId);
        InputStreamResource resource =
                new InputStreamResource(
                        objectStorageService.downloadFile(
                                userDocumentsBucket, document.getStorageKey()));
        return new DocumentDownload(
                resource,
                document.getFileName(),
                AllowedAttachmentMediaTypes.forDelivery(document.getContentType()));
    }

    /**
     * Deletes the user documents that belong to the given link targets ({@code doc_document_links})
     * and removes the targets' links from documents that still belong elsewhere. Record links
     * (certification, training course, driving license) own a document; workflow and onboarding
     * links only own documents that no other link claims, because promoted documents keep their
     * source link next to the record link. Signed entry documents referenced by an entry-document
     * fulfillment are never deleted here. Links and tags cascade from {@code doc_documents}; S3
     * objects go through the deletion outbox. Set-based, so retention handlers can call it.
     *
     * @return number of deleted documents
     */
    @Transactional
    public int deleteDocumentsForLinkTargets(
            Long tenantId, AppLinkType linkType, Collection<Long> linkIds) {
        if (linkType == AppLinkType.CERTIFICATION_RECORD) {
            throw new IllegalArgumentException(
                    "CERTIFICATION_RECORD links carry the definition id shared by all records;"
                            + " use deleteDocumentsByIds with the record's documents");
        }
        if (linkIds == null || linkIds.isEmpty()) {
            return 0;
        }
        Set<String> retainingLinkTypes = retainingLinkTypes(linkType);
        int deleted = 0;
        for (List<Long> chunk : chunks(linkIds)) {
            userDocumentRepository
                    .findStorageKeysOfDocumentsOwnedByLinkTargets(
                            linkType.name(), chunk, retainingLinkTypes)
                    .forEach(key -> enqueueObjectDeletion(tenantId, key));
            deleted +=
                    userDocumentRepository.deleteDocumentsOwnedByLinkTargets(
                            linkType.name(), chunk, retainingLinkTypes);
            userDocumentRepository.deleteLinksToTargets(linkType.name(), chunk);
        }
        if (deleted > 0) {
            log.info("Deleted {} user document(s) linked to {}", deleted, linkType);
        }
        return deleted;
    }

    /**
     * Deletes specific user documents and their S3 objects, for owners whose document link id is
     * not the record id (certification records link by definition id). The caller decides
     * ownership.
     *
     * @return number of deleted documents
     */
    @Transactional
    public int deleteDocumentsByIds(Long tenantId, Collection<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return 0;
        }
        int deleted = 0;
        for (List<Long> chunk : chunks(documentIds)) {
            userDocumentRepository
                    .findStorageKeysByIdIn(chunk)
                    .forEach(key -> enqueueObjectDeletion(tenantId, key));
            deleted += userDocumentRepository.deleteByIdIn(chunk);
        }
        if (deleted > 0) {
            log.info("Deleted {} user document(s) by id", deleted);
        }
        return deleted;
    }

    /** Checks the user's tenant and active organization membership. */
    @Transactional(readOnly = true)
    public void assertUserBelongsToOrganization(Long userId, Long organizationId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));
        Long tenantId = requireTenantId();
        if (!user.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("User does not belong to the tenant");
        }
        if (!userOrganizationMembershipRepository.existsActiveByUser_IdAndOrganization_Id(
                userId, organizationId, Instant.now())) {
            throw new IllegalArgumentException(
                    "User does not belong to the specified organization");
        }
    }

    private UserDocument findOwnedDocument(Long userId, Long documentId) {
        return userDocumentRepository
                .findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
    }

    private UserDocumentPageDto toPageDto(Page<UserDocument> page) {
        return UserDocumentPageDto.builder()
                .content(page.getContent().stream().map(this::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    private UserDocumentDto toDto(UserDocument document) {
        List<String> tags =
                document.getTags().stream()
                        .map(UserDocumentTag::getTag)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList();
        List<UserDocumentLinkDto> links =
                document.getLinks().stream()
                        .sorted(
                                Comparator.comparing(UserDocumentLink::getLinkType)
                                        .thenComparing(UserDocumentLink::getLinkId))
                        .map(linkResolver::toDto)
                        .toList();
        String displayTitle =
                StringUtils.hasText(document.getTitle())
                        ? document.getTitle()
                        : document.getFileName();
        return UserDocumentDto.builder()
                .id(document.getId())
                .title(displayTitle)
                .description(document.getDescription())
                .fileName(document.getFileName())
                .contentType(document.getContentType())
                .sizeBytes(document.getSizeBytes())
                .tags(tags)
                .links(links)
                .viewedAt(document.getViewedAt())
                .build();
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size must be less than 50MB");
        }
        EntityAttachmentService.validateContentType(file, AllowedAttachmentMediaTypes.VALUES);
    }

    static Set<String> retainingLinkTypes(AppLinkType deletedLinkType) {
        Set<AppLinkType> retaining =
                RECORD_LINK_TYPES.contains(deletedLinkType)
                        ? RECORD_LINK_TYPES
                        : EnumSet.allOf(AppLinkType.class);
        return retaining.stream().map(Enum::name).collect(Collectors.toSet());
    }

    private void enqueueObjectDeletion(Long tenantId, String storageKey) {
        deletionOutboxService.enqueueObjectDeletion(tenantId, userDocumentsBucket, storageKey);
    }

    private static List<List<Long>> chunks(Collection<Long> ids) {
        List<Long> distinct = List.copyOf(new LinkedHashSet<>(ids));
        List<List<Long>> chunks = new ArrayList<>();
        for (int start = 0; start < distinct.size(); start += DELETE_CHUNK_SIZE) {
            chunks.add(
                    distinct.subList(start, Math.min(start + DELETE_CHUNK_SIZE, distinct.size())));
        }
        return chunks;
    }

    private Long requireTenantId() {
        Long tenantId = tokenService.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No tenantId found in token");
        }
        return tenantId;
    }

    public record DocumentDownload(
            InputStreamResource resource, String fileName, String contentType) {}
}
