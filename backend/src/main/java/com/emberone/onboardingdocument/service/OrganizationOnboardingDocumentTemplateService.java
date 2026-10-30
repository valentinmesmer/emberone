/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.service;

import com.emberone.attachment.AllowedAttachmentMediaTypes;
import com.emberone.attachment.service.EntityAttachmentService;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinition;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinitionRepository;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinitionTemplateVersion;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinitionTemplateVersionRepository;
import com.emberone.onboardingdocument.repository.UserEntryDocumentFulfillmentEventRepository;
import com.emberone.onboardingdocument.repository.UserEntryDocumentFulfillmentRepository;
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
public class OrganizationOnboardingDocumentTemplateService {

    private final OrganizationOnboardingDocumentDefinitionRepository definitionRepository;
    private final OrganizationOnboardingDocumentDefinitionTemplateVersionRepository
            versionRepository;
    private final UserEntryDocumentFulfillmentEventRepository fulfillmentEventRepository;
    private final UserEntryDocumentFulfillmentRepository fulfillmentRepository;
    private final ObjectStorageService objectStorageService;
    private final ObjectStorageTransactionCoordinator objectStorageTransactionCoordinator;

    @Value("${object-storage.attachments-bucket}")
    private String attachmentsBucket;

    @Transactional(readOnly = true)
    public boolean hasCurrentTemplate(OrganizationOnboardingDocumentDefinition definition) {
        return findCurrentVersion(definition).isPresent();
    }

    @Transactional(readOnly = true)
    public Optional<OrganizationOnboardingDocumentDefinitionTemplateVersion> findCurrentVersion(
            OrganizationOnboardingDocumentDefinition definition) {
        if (definition.getVersion() <= 0) {
            return Optional.empty();
        }
        return versionRepository.findByOrgDocumentDefinitionIdAndVersion(
                definition.getId(), definition.getVersion());
    }

    @Transactional(readOnly = true)
    public Optional<OnboardingDocumentTemplateService.TemplateMetadata> findCurrentTemplateMetadata(
            OrganizationOnboardingDocumentDefinition definition) {
        return findCurrentVersion(definition)
                .map(
                        version ->
                                new OnboardingDocumentTemplateService.TemplateMetadata(
                                        version.getFileName(),
                                        version.getSizeBytes(),
                                        version.getSha256Hex(),
                                        version.getVersion(),
                                        version.getUploadedAt()));
    }

    @Transactional
    public OrganizationOnboardingDocumentDefinition uploadTemplate(
            long tenantId,
            long organizationId,
            String configKey,
            MultipartFile file,
            Long uploadedByUserId) {
        return withTenantContext(
                tenantId,
                () -> {
                    OrganizationOnboardingDocumentDefinition definition =
                            requireActiveDefinition(organizationId, configKey);
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
                                    + "/organizations/"
                                    + organizationId
                                    + "/onboarding-documents/"
                                    + definition.getId()
                                    + "/"
                                    + UUID.randomUUID()
                                    + extension;
                    objectStorageTransactionCoordinator.uploadFileWithRollbackCleanup(
                            file, attachmentsBucket, storageKey);
                    Instant uploadedAt = Instant.now();
                    versionRepository.save(
                            OrganizationOnboardingDocumentDefinitionTemplateVersion.builder()
                                    .tenantId(tenantId)
                                    .organizationId(organizationId)
                                    .orgDocumentDefinitionId(definition.getId())
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
                    OrganizationOnboardingDocumentDefinition saved =
                            definitionRepository.save(definition);
                    log.info(
                            "Uploaded template for org onboarding document '{}' in org {}",
                            configKey,
                            organizationId);
                    return saved;
                });
    }

    @Transactional
    public void deleteTemplate(long tenantId, long organizationId, String configKey) {
        withTenantContext(
                tenantId,
                () -> {
                    OrganizationOnboardingDocumentDefinition definition =
                            requireActiveDefinition(organizationId, configKey);
                    if (!hasCurrentTemplate(definition)) {
                        return null;
                    }
                    definition.setVersion(0);
                    definition.setActive(false);
                    definitionRepository.save(definition);
                    deleteUnreferencedVersionStorage(definition.getId());
                    log.info(
                            "Deleted template for org onboarding document '{}' in org {}",
                            configKey,
                            organizationId);
                    return null;
                });
    }

    @Transactional(readOnly = true)
    public TemplateDownload downloadTemplate(long tenantId, long organizationId, String configKey) {
        return withTenantContext(
                tenantId,
                () -> {
                    OrganizationOnboardingDocumentDefinition definition =
                            requireDefinition(organizationId, configKey);
                    OrganizationOnboardingDocumentDefinitionTemplateVersion version =
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

    void deleteStoredTemplateForDefinition(OrganizationOnboardingDocumentDefinition definition) {
        deleteUnreferencedVersionStorage(definition.getId());
    }

    private void deleteUnreferencedVersionStorage(long orgDocumentDefinitionId) {
        List<OrganizationOnboardingDocumentDefinitionTemplateVersion> versions =
                versionRepository.findByOrgDocumentDefinitionIdOrderByVersionAsc(
                        orgDocumentDefinitionId);
        for (OrganizationOnboardingDocumentDefinitionTemplateVersion version : versions) {
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

    private int resolveNextVersionNumber(OrganizationOnboardingDocumentDefinition definition) {
        if (definition.getVersion() > 0
                && versionRepository.existsByOrgDocumentDefinitionIdAndVersion(
                        definition.getId(), definition.getVersion())) {
            return definition.getVersion() + 1;
        }
        return versionRepository.findMaxVersion(definition.getId()) + 1;
    }

    private OrganizationOnboardingDocumentDefinition requireDefinition(
            long organizationId, String configKey) {
        return definitionRepository
                .findByOrganizationIdAndConfigKey(organizationId, configKey)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Onboarding document not found: " + configKey));
    }

    private OrganizationOnboardingDocumentDefinition requireActiveDefinition(
            long organizationId, String configKey) {
        OrganizationOnboardingDocumentDefinition definition =
                requireDefinition(organizationId, configKey);
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
}
