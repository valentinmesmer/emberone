/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.service;

import com.emberone.attachment.AllowedAttachmentMediaTypes;
import com.emberone.attachment.service.EntityAttachmentService;
import com.emberone.onboardingdocument.repository.UserEntryDocumentFulfillmentEventRepository;
import com.emberone.onboardingdocument.repository.UserEntryDocumentFulfillmentRepository;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinition;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinitionRepository;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinitionTemplateVersion;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinitionTemplateVersionRepository;
import com.emberone.onboardingdocument.util.OnboardingDocumentTemplateDigest;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.platform.storage.service.ObjectStorageService;
import com.emberone.platform.storage.service.ObjectStorageTransactionCoordinator;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingDocumentTemplateService {

    private final TenantOnboardingDocumentDefinitionRepository definitionRepository;
    private final TenantOnboardingDocumentDefinitionTemplateVersionRepository versionRepository;
    private final UserEntryDocumentFulfillmentEventRepository fulfillmentEventRepository;
    private final UserEntryDocumentFulfillmentRepository fulfillmentRepository;
    private final ObjectStorageService objectStorageService;
    private final ObjectStorageTransactionCoordinator objectStorageTransactionCoordinator;

    @Value("${object-storage.attachments-bucket}")
    private String attachmentsBucket;

    @Transactional(readOnly = true)
    public boolean hasCurrentTemplate(TenantOnboardingDocumentDefinition definition) {
        return findCurrentVersion(definition).isPresent();
    }

    @Transactional(readOnly = true)
    public Optional<TenantOnboardingDocumentDefinitionTemplateVersion> findCurrentVersion(
            TenantOnboardingDocumentDefinition definition) {
        if (definition.getVersion() <= 0) {
            return Optional.empty();
        }
        return versionRepository.findByDocumentDefinitionIdAndVersion(
                definition.getId(), definition.getVersion());
    }

    @Transactional(readOnly = true)
    public Optional<TemplateMetadata> findCurrentTemplateMetadata(
            TenantOnboardingDocumentDefinition definition) {
        return findCurrentVersion(definition).map(TemplateMetadata::from);
    }

    @Transactional
    public TenantOnboardingDocumentDefinition uploadTemplate(
            long tenantId, String configKey, MultipartFile file, Long uploadedByUserId) {
        return withTenantContext(
                tenantId,
                () -> {
                    TenantOnboardingDocumentDefinition definition =
                            requireActiveDefinition(configKey);
                    EntityAttachmentService.validateFile(file);
                    EntityAttachmentService.validateContentType(
                            file, AllowedAttachmentMediaTypes.VALUES);
                    String sha256Hex = OnboardingDocumentTemplateDigest.sha256Hex(file);
                    int newVersion = resolveNextVersionNumber(definition);
                    String extension =
                            EntityAttachmentService.getFileExtension(file.getOriginalFilename());
                    String storageKey =
                            "tenants/"
                                    + tenantId
                                    + "/onboarding-documents/"
                                    + definition.getId()
                                    + "/"
                                    + UUID.randomUUID()
                                    + extension;
                    objectStorageTransactionCoordinator.uploadFileWithRollbackCleanup(
                            file, attachmentsBucket, storageKey);
                    Instant uploadedAt = Instant.now();
                    versionRepository.save(
                            TenantOnboardingDocumentDefinitionTemplateVersion.builder()
                                    .tenantId(tenantId)
                                    .documentDefinitionId(definition.getId())
                                    .version(newVersion)
                                    .storageKey(storageKey)
                                    .sha256Hex(sha256Hex)
                                    .fileName(
                                            file.getOriginalFilename() != null
                                                    ? file.getOriginalFilename()
                                                    : "document")
                                    .contentType(
                                            file.getContentType() != null
                                                    ? file.getContentType()
                                                    : "application/octet-stream")
                                    .sizeBytes(file.getSize())
                                    .uploadedByUserId(uploadedByUserId)
                                    .uploadedAt(uploadedAt)
                                    .build());
                    definition.setVersion(newVersion);
                    TenantOnboardingDocumentDefinition saved =
                            definitionRepository.save(definition);
                    log.info("Uploaded template for onboarding document '{}'", configKey);
                    return saved;
                });
    }

    @Transactional
    public void deleteTemplate(long tenantId, String configKey) {
        withTenantContext(
                tenantId,
                () -> {
                    TenantOnboardingDocumentDefinition definition =
                            requireActiveDefinition(configKey);
                    if (!hasCurrentTemplate(definition)) {
                        return null;
                    }
                    definition.setVersion(0);
                    definition.setActive(false);
                    definitionRepository.save(definition);
                    deleteUnreferencedVersionStorage(definition.getId());
                    log.info("Deleted template for onboarding document '{}'", configKey);
                    return null;
                });
    }

    @Transactional(readOnly = true)
    public TemplateDownload downloadTemplate(long tenantId, String configKey) {
        return withTenantContext(
                tenantId,
                () -> {
                    TenantOnboardingDocumentDefinition definition = requireDefinition(configKey);
                    TenantOnboardingDocumentDefinitionTemplateVersion version =
                            findCurrentVersion(definition)
                                    .orElseThrow(
                                            () ->
                                                    new ResourceNotFoundException(
                                                            "Onboarding document template not found: "
                                                                    + configKey));
                    InputStream inputStream =
                            objectStorageService.downloadFile(
                                    attachmentsBucket, version.getStorageKey());
                    return new TemplateDownload(
                            inputStream,
                            version.getFileName(),
                            AllowedAttachmentMediaTypes.forDelivery(version.getContentType()));
                });
    }

    void deleteStoredTemplateForDefinition(TenantOnboardingDocumentDefinition definition) {
        deleteUnreferencedVersionStorage(definition.getId());
    }

    private void deleteUnreferencedVersionStorage(long documentDefinitionId) {
        List<TenantOnboardingDocumentDefinitionTemplateVersion> versions =
                versionRepository.findByDocumentDefinitionIdOrderByVersionAsc(documentDefinitionId);
        for (TenantOnboardingDocumentDefinitionTemplateVersion version : versions) {
            if (isTemplateVersionReferenced(version.getSha256Hex())) {
                continue;
            }
            objectStorageTransactionCoordinator.deleteFileAfterCommit(
                    attachmentsBucket, version.getStorageKey());
        }
    }

    private boolean isTemplateVersionReferenced(String sha256Hex) {
        if (fulfillmentEventRepository.existsByTemplateSha256Hex(sha256Hex)) {
            return true;
        }
        return fulfillmentRepository.existsByTemplateSha256Hex(sha256Hex);
    }

    private int resolveNextVersionNumber(TenantOnboardingDocumentDefinition definition) {
        if (definition.getVersion() > 0
                && versionRepository.existsByDocumentDefinitionIdAndVersion(
                        definition.getId(), definition.getVersion())) {
            return definition.getVersion() + 1;
        }
        return versionRepository.findMaxVersion(definition.getId()) + 1;
    }

    private TenantOnboardingDocumentDefinition requireDefinition(String configKey) {
        return definitionRepository
                .findByConfigKey(configKey)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Onboarding document not found: " + configKey));
    }

    private TenantOnboardingDocumentDefinition requireActiveDefinition(String configKey) {
        TenantOnboardingDocumentDefinition definition = requireDefinition(configKey);
        if (definition.isArchived()) {
            throw new ResourceNotFoundException("Onboarding document not found: " + configKey);
        }
        return definition;
    }

    private <T> T withTenantContext(long tenantId, Supplier<T> action) {
        TenantScopeGuard.bind(tenantId);
        TenantScopeGuard.requireActiveTenantMatches(tenantId);
        return action.get();
    }

    public record TemplateDownload(InputStream inputStream, String fileName, String contentType) {}

    public record TemplateMetadata(
            String fileName, long sizeBytes, String sha256Hex, int version, Instant uploadedAt) {

        static TemplateMetadata from(TenantOnboardingDocumentDefinitionTemplateVersion version) {
            return new TemplateMetadata(
                    version.getFileName(),
                    version.getSizeBytes(),
                    version.getSha256Hex(),
                    version.getVersion(),
                    version.getUploadedAt());
        }
    }
}
