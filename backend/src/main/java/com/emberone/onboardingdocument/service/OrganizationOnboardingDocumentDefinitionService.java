/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.service;

import com.emberone.onboarding.domain.OnboardingStepType;
import com.emberone.onboarding.standard.StandardOnboardingStepRepository;
import com.emberone.onboardingdocument.dto.CreateOrganizationOnboardingDocumentDefinitionRequest;
import com.emberone.onboardingdocument.dto.OrganizationOnboardingDocumentDefinitionDto;
import com.emberone.onboardingdocument.dto.UpdateOrganizationOnboardingDocumentDefinitionRequest;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinition;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinitionRepository;
import com.emberone.onboardingdocument.repository.UserEntryDocumentFulfillmentEventRepository;
import com.emberone.onboardingdocument.repository.UserEntryDocumentFulfillmentRepository;
import com.emberone.onboardingdocument.util.OnboardingDocumentAbbreviation;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.service.EffectiveStandardResolver;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationOnboardingDocumentDefinitionService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationOnboardingDocumentDefinitionRepository definitionRepository;
    private final OrganizationOnboardingDocumentTemplateService templateService;
    private final UserEntryDocumentFulfillmentRepository fulfillmentRepository;
    private final UserEntryDocumentFulfillmentEventRepository fulfillmentEventRepository;
    private final EffectiveStandardResolver effectiveStandardResolver;
    private final StandardOnboardingStepRepository standardOnboardingStepRepository;

    @Transactional(readOnly = true)
    public boolean isOrganizationEntryDocumentsStepEnabled(long tenantId, long organizationId) {
        return withTenantAndOrganizationContext(
                tenantId,
                organizationId,
                () -> {
                    Organization organization =
                            requireOrganizationInTenant(tenantId, organizationId);
                    return effectiveStandardResolver
                            .resolveForOrganization(organization)
                            .map(this::hasOrganizationEntryDocumentsStep)
                            .orElse(false);
                });
    }

    @Transactional(readOnly = true)
    public List<OrganizationOnboardingDocumentDefinitionDto> list(
            long tenantId, long organizationId) {
        return withTenantAndOrganizationContext(
                tenantId,
                organizationId,
                () -> {
                    requireOrganizationEntryDocumentsStepEnabled(tenantId, organizationId);
                    return definitionRepository
                            .findByOrganizationIdOrderByNameAsc(organizationId)
                            .stream()
                            .map(this::toDto)
                            .toList();
                });
    }

    @Transactional(readOnly = true)
    public OrganizationOnboardingDocumentDefinitionDto get(
            long tenantId, long organizationId, String configKey) {
        return withTenantAndOrganizationContext(
                tenantId,
                organizationId,
                () -> {
                    requireOrganizationEntryDocumentsStepEnabled(tenantId, organizationId);
                    return toDto(requireDefinition(organizationId, configKey));
                });
    }

    @Transactional
    public OrganizationOnboardingDocumentDefinitionDto create(
            long tenantId,
            long organizationId,
            CreateOrganizationOnboardingDocumentDefinitionRequest request) {
        return withTenantAndOrganizationContext(
                tenantId,
                organizationId,
                () -> {
                    requireOrganizationEntryDocumentsStepEnabled(tenantId, organizationId);
                    String abbreviation =
                            OnboardingDocumentAbbreviation.requireValid(request.getAbbreviation());
                    if (definitionRepository.existsByOrganizationIdAndConfigKeyAndArchivedFalse(
                            organizationId, abbreviation)) {
                        throw new ConflictException(
                                "Onboarding document abbreviation already exists: " + abbreviation);
                    }
                    OrganizationOnboardingDocumentDefinition saved =
                            definitionRepository.save(
                                    OrganizationOnboardingDocumentDefinition.builder()
                                            .tenantId(tenantId)
                                            .organizationId(organizationId)
                                            .configKey(abbreviation)
                                            .name(request.getName().trim())
                                            .description(
                                                    normalizeDescription(request.getDescription()))
                                            .fulfillmentMode(request.getFulfillmentMode())
                                            .mandatory(request.isMandatory())
                                            .active(false)
                                            .build());
                    log.info(
                            "Created org onboarding document '{}' for org {}",
                            abbreviation,
                            organizationId);
                    return toDto(saved);
                });
    }

    @Transactional
    public OrganizationOnboardingDocumentDefinitionDto update(
            long tenantId,
            long organizationId,
            String configKey,
            UpdateOrganizationOnboardingDocumentDefinitionRequest request) {
        return withTenantAndOrganizationContext(
                tenantId,
                organizationId,
                () -> {
                    requireOrganizationEntryDocumentsStepEnabled(tenantId, organizationId);
                    OrganizationOnboardingDocumentDefinition definition =
                            requireActiveDefinition(organizationId, configKey);
                    String abbreviation =
                            OnboardingDocumentAbbreviation.requireValid(request.getAbbreviation());
                    if (definitionRepository
                            .existsByOrganizationIdAndConfigKeyAndArchivedFalseAndIdNot(
                                    organizationId, abbreviation, definition.getId())) {
                        throw new ConflictException(
                                "Onboarding document abbreviation already exists: " + abbreviation);
                    }
                    definition.setConfigKey(abbreviation);
                    definition.setName(request.getName().trim());
                    definition.setDescription(normalizeDescription(request.getDescription()));
                    definition.setFulfillmentMode(request.getFulfillmentMode());
                    definition.setMandatory(request.isMandatory());
                    applyRequestedActive(definition, request.isActive());
                    OrganizationOnboardingDocumentDefinition saved =
                            definitionRepository.save(definition);
                    log.info(
                            "Updated org onboarding document '{}' for org {}",
                            configKey,
                            organizationId);
                    return toDto(saved);
                });
    }

    @Transactional
    public void delete(long tenantId, long organizationId, String configKey) {
        withTenantAndOrganizationContext(
                tenantId,
                organizationId,
                () -> {
                    requireOrganizationEntryDocumentsStepEnabled(tenantId, organizationId);
                    OrganizationOnboardingDocumentDefinition definition =
                            requireDefinition(organizationId, configKey);
                    if (!isDeletable(definition)) {
                        throw new ConflictException(
                                "Onboarding document is in use and cannot be deleted");
                    }
                    templateService.deleteStoredTemplateForDefinition(definition);
                    definitionRepository.delete(definition);
                    log.info(
                            "Deleted org onboarding document '{}' for org {}",
                            configKey,
                            organizationId);
                    return null;
                });
    }

    @Transactional
    public void archive(long tenantId, long organizationId, String configKey) {
        withTenantAndOrganizationContext(
                tenantId,
                organizationId,
                () -> {
                    requireOrganizationEntryDocumentsStepEnabled(tenantId, organizationId);
                    OrganizationOnboardingDocumentDefinition definition =
                            requireActiveDefinition(organizationId, configKey);
                    if (!isArchivable(definition)) {
                        throw new ConflictException("Onboarding document cannot be archived");
                    }
                    archiveDefinition(definition);
                    return null;
                });
    }

    @Transactional
    public void unarchive(long tenantId, long organizationId, String configKey) {
        withTenantAndOrganizationContext(
                tenantId,
                organizationId,
                () -> {
                    requireOrganizationEntryDocumentsStepEnabled(tenantId, organizationId);
                    OrganizationOnboardingDocumentDefinition definition =
                            requireDefinition(organizationId, configKey);
                    if (!definition.isArchived()) {
                        throw new ConflictException("Onboarding document is not archived");
                    }
                    if (definitionRepository
                            .existsByOrganizationIdAndConfigKeyAndArchivedFalseAndIdNot(
                                    organizationId,
                                    definition.getConfigKey(),
                                    definition.getId())) {
                        throw new ConflictException(
                                "Onboarding document abbreviation already exists: "
                                        + definition.getConfigKey());
                    }
                    unarchiveDefinition(definition);
                    return null;
                });
    }

    @Transactional
    public OrganizationOnboardingDocumentDefinitionDto uploadTemplate(
            long tenantId,
            long organizationId,
            String configKey,
            MultipartFile file,
            Long uploadedByUserId) {
        requireOrganizationEntryDocumentsStepEnabled(tenantId, organizationId);
        return toDto(
                templateService.uploadTemplate(
                        tenantId, organizationId, configKey, file, uploadedByUserId));
    }

    @Transactional
    public OrganizationOnboardingDocumentDefinitionDto deleteTemplate(
            long tenantId, long organizationId, String configKey) {
        requireOrganizationEntryDocumentsStepEnabled(tenantId, organizationId);
        templateService.deleteTemplate(tenantId, organizationId, configKey);
        return get(tenantId, organizationId, configKey);
    }

    @Transactional(readOnly = true)
    public OrganizationOnboardingDocumentTemplateService.TemplateDownload downloadTemplate(
            long tenantId, long organizationId, String configKey) {
        requireOrganizationEntryDocumentsStepEnabled(tenantId, organizationId);
        return templateService.downloadTemplate(tenantId, organizationId, configKey);
    }

    @Transactional(readOnly = true)
    public List<String> listActiveConfigKeys(long organizationId) {
        return definitionRepository
                .findByOrganizationIdAndActiveTrueAndArchivedFalseOrderByNameAsc(organizationId)
                .stream()
                .filter(templateService::hasCurrentTemplate)
                .map(OrganizationOnboardingDocumentDefinition::getConfigKey)
                .toList();
    }

    private void archiveDefinition(OrganizationOnboardingDocumentDefinition definition) {
        Instant now = Instant.now();
        LifecycleFields.archiveCatalogEntry(definition, now);
        definitionRepository.save(definition);
        log.info(
                "Archived org onboarding document '{}' for org {}",
                definition.getConfigKey(),
                definition.getOrganizationId());
    }

    private void unarchiveDefinition(OrganizationOnboardingDocumentDefinition definition) {
        LifecycleFields.unarchiveCatalogEntry(
                definition, templateService.hasCurrentTemplate(definition));
        definitionRepository.save(definition);
        log.info(
                "Unarchived org onboarding document '{}' for org {}",
                definition.getConfigKey(),
                definition.getOrganizationId());
    }

    private boolean isDeletable(OrganizationOnboardingDocumentDefinition definition) {
        long organizationId = definition.getOrganizationId();
        String configKey = definition.getConfigKey();
        return !fulfillmentRepository.existsByOrganizationIdAndConfigKey(organizationId, configKey)
                && !fulfillmentEventRepository.existsByOrganizationIdAndConfigKey(
                        organizationId, configKey);
    }

    private boolean isArchivable(OrganizationOnboardingDocumentDefinition definition) {
        return !definition.isArchived() && !isDeletable(definition);
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

    private static String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }

    private void applyRequestedActive(
            OrganizationOnboardingDocumentDefinition definition, boolean requestedActive) {
        if (requestedActive && !templateService.hasCurrentTemplate(definition)) {
            throw new ConflictException(
                    "Onboarding document cannot be active without a template file");
        }
        definition.setActive(requestedActive);
    }

    private OrganizationOnboardingDocumentDefinitionDto toDto(
            OrganizationOnboardingDocumentDefinition definition) {
        var templateMetadata = templateService.findCurrentTemplateMetadata(definition);
        return OrganizationOnboardingDocumentDefinitionDto.builder()
                .id(definition.getId())
                .organizationId(definition.getOrganizationId())
                .configKey(definition.getConfigKey())
                .abbreviation(definition.getConfigKey())
                .name(definition.getName())
                .description(definition.getDescription())
                .fulfillmentMode(definition.getFulfillmentMode())
                .mandatory(definition.isMandatory())
                .active(definition.isActive())
                .archived(definition.isArchived())
                .deletable(isDeletable(definition))
                .archivable(isArchivable(definition))
                .hasTemplate(templateMetadata.isPresent())
                .templateFileName(templateMetadata.map(m -> m.fileName()).orElse(null))
                .templateSizeBytes(templateMetadata.map(m -> m.sizeBytes()).orElse(null))
                .build();
    }

    private com.emberone.organization.domain.Organization requireOrganizationInTenant(
            long tenantId, long organizationId) {
        com.emberone.organization.domain.Organization organization =
                organizationRepository
                        .findById(organizationId)
                        .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        TenantScopeGuard.requireEntityInActiveTenant(
                organization.getTenantId(), "Organization", organizationId);
        if (!organization.getTenantId().equals(tenantId)) {
            throw new OrganizationNotFoundException(organizationId);
        }
        return organization;
    }

    private void requireOrganizationEntryDocumentsStepEnabled(long tenantId, long organizationId) {
        if (!isOrganizationEntryDocumentsStepEnabled(tenantId, organizationId)) {
            throw new ConflictException(
                    "Organization entry documents step is not enabled for this organization");
        }
    }

    private boolean hasOrganizationEntryDocumentsStep(Standard standard) {
        return standardOnboardingStepRepository.existsByStandardIdAndStepType(
                standard.getId(), OnboardingStepType.ORGANIZATION_ENTRY_DOCUMENTS);
    }

    private <T> T withTenantAndOrganizationContext(
            long tenantId, long organizationId, Supplier<T> action) {
        TenantScopeGuard.bind(tenantId);
        TenantScopeGuard.requireActiveTenantMatches(tenantId);
        requireOrganizationInTenant(tenantId, organizationId);
        return action.get();
    }
}
