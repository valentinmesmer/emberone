/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.service;

import com.emberone.federalstate.baseline.OnboardingDocumentDefinitionBaseline;
import com.emberone.onboarding.service.StandardOnboardingStepService;
import com.emberone.onboardingdocument.dto.CreateTenantOnboardingDocumentDefinitionRequest;
import com.emberone.onboardingdocument.dto.TenantOnboardingDocumentDefinitionDto;
import com.emberone.onboardingdocument.dto.UpdateTenantOnboardingDocumentDefinitionRequest;
import com.emberone.onboardingdocument.repository.UserEntryDocumentFulfillmentEventRepository;
import com.emberone.onboardingdocument.repository.UserEntryDocumentFulfillmentRepository;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinition;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinitionRepository;
import com.emberone.onboardingdocument.util.OnboardingDocumentAbbreviation;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.security.TenantScopeGuard;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantOnboardingDocumentDefinitionService {

    private final TenantOnboardingDocumentDefinitionRepository definitionRepository;
    private final StandardOnboardingStepService standardOnboardingStepService;
    private final OnboardingDocumentTemplateService templateService;
    private final UserEntryDocumentFulfillmentRepository fulfillmentRepository;
    private final UserEntryDocumentFulfillmentEventRepository fulfillmentEventRepository;

    @Transactional(readOnly = true)
    public List<TenantOnboardingDocumentDefinitionDto> list(long tenantId) {
        return withTenantContext(
                tenantId,
                () ->
                        definitionRepository.findAllByOrderByNameAsc().stream()
                                .map(this::toDto)
                                .toList());
    }

    @Transactional(readOnly = true)
    public TenantOnboardingDocumentDefinitionDto get(long tenantId, String configKey) {
        return withTenantContext(tenantId, () -> toDto(requireDefinition(configKey)));
    }

    @Transactional
    public TenantOnboardingDocumentDefinitionDto create(
            long tenantId, CreateTenantOnboardingDocumentDefinitionRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    String abbreviation =
                            OnboardingDocumentAbbreviation.requireValid(request.getAbbreviation());
                    if (definitionRepository.existsByConfigKeyAndArchivedFalse(abbreviation)) {
                        throw new ConflictException(
                                "Onboarding document abbreviation already exists: " + abbreviation);
                    }
                    TenantOnboardingDocumentDefinition saved =
                            definitionRepository.save(
                                    TenantOnboardingDocumentDefinition.builder()
                                            .tenantId(tenantId)
                                            .configKey(abbreviation)
                                            .name(request.getName().trim())
                                            .description(
                                                    normalizeDescription(request.getDescription()))
                                            .fulfillmentMode(request.getFulfillmentMode())
                                            .mandatory(request.isMandatory())
                                            .active(false)
                                            .build());
                    log.info("Created tenant onboarding document '{}'", abbreviation);
                    return toDto(saved);
                });
    }

    @Transactional
    public void seedBaselineDefinitionsIfMissing(
            long tenantId, List<OnboardingDocumentDefinitionBaseline> items) {
        withTenantContext(
                tenantId,
                () -> {
                    for (OnboardingDocumentDefinitionBaseline item : items) {
                        if (definitionRepository.findByConfigKey(item.configKey()).isPresent()) {
                            continue;
                        }
                        definitionRepository.save(
                                TenantOnboardingDocumentDefinition.builder()
                                        .tenantId(tenantId)
                                        .configKey(item.configKey())
                                        .name(item.name())
                                        .description(item.description())
                                        .fulfillmentMode(item.fulfillmentMode())
                                        .mandatory(item.mandatory())
                                        .active(false)
                                        .build());
                    }
                    return null;
                });
    }

    @Transactional
    public TenantOnboardingDocumentDefinitionDto update(
            long tenantId,
            String configKey,
            UpdateTenantOnboardingDocumentDefinitionRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    TenantOnboardingDocumentDefinition definition =
                            requireActiveDefinition(configKey);
                    String abbreviation =
                            OnboardingDocumentAbbreviation.requireValid(request.getAbbreviation());
                    if (definitionRepository.existsByConfigKeyAndArchivedFalseAndIdNot(
                            abbreviation, definition.getId())) {
                        throw new ConflictException(
                                "Onboarding document abbreviation already exists: " + abbreviation);
                    }
                    retargetConfigKey(definition.getConfigKey(), abbreviation);
                    definition.setConfigKey(abbreviation);
                    definition.setName(request.getName().trim());
                    definition.setDescription(normalizeDescription(request.getDescription()));
                    definition.setFulfillmentMode(request.getFulfillmentMode());
                    definition.setMandatory(request.isMandatory());
                    applyRequestedActive(definition, request.isActive());
                    TenantOnboardingDocumentDefinition saved =
                            definitionRepository.save(definition);
                    log.info("Updated tenant onboarding document '{}'", configKey);
                    return toDto(saved);
                });
    }

    @Transactional
    public void delete(long tenantId, String configKey) {
        withTenantContext(
                tenantId,
                () -> {
                    TenantOnboardingDocumentDefinition definition = requireDefinition(configKey);
                    if (!isDeletable(definition)) {
                        throw new ConflictException(
                                "Onboarding document is in use and cannot be deleted");
                    }
                    standardOnboardingStepService.removeDocumentConfigKey(configKey);
                    templateService.deleteStoredTemplateForDefinition(definition);
                    definitionRepository.delete(definition);
                    log.info("Deleted tenant onboarding document '{}'", configKey);
                    return null;
                });
    }

    @Transactional
    public void archive(long tenantId, String configKey) {
        withTenantContext(
                tenantId,
                () -> {
                    TenantOnboardingDocumentDefinition definition =
                            requireActiveDefinition(configKey);
                    if (!isArchivable(definition)) {
                        throw new ConflictException("Onboarding document cannot be archived");
                    }
                    standardOnboardingStepService.removeDocumentConfigKey(configKey);
                    archiveDefinition(definition);
                    return null;
                });
    }

    @Transactional
    public void unarchive(long tenantId, String configKey) {
        withTenantContext(
                tenantId,
                () -> {
                    TenantOnboardingDocumentDefinition definition = requireDefinition(configKey);
                    if (!definition.isArchived()) {
                        throw new ConflictException("Onboarding document is not archived");
                    }
                    if (definitionRepository.existsByConfigKeyAndArchivedFalseAndIdNot(
                            definition.getConfigKey(), definition.getId())) {
                        throw new ConflictException(
                                "Onboarding document abbreviation already exists: "
                                        + definition.getConfigKey());
                    }
                    unarchiveDefinition(definition);
                    return null;
                });
    }

    @Transactional
    public TenantOnboardingDocumentDefinitionDto uploadTemplate(
            long tenantId,
            String configKey,
            org.springframework.web.multipart.MultipartFile file,
            Long uploadedByUserId) {
        return withTenantContext(
                tenantId,
                () ->
                        toDto(
                                templateService.uploadTemplate(
                                        tenantId, configKey, file, uploadedByUserId)));
    }

    @Transactional
    public TenantOnboardingDocumentDefinitionDto deleteTemplate(long tenantId, String configKey) {
        templateService.deleteTemplate(tenantId, configKey);
        return get(tenantId, configKey);
    }

    @Transactional(readOnly = true)
    public OnboardingDocumentTemplateService.TemplateDownload downloadTemplate(
            long tenantId, String configKey) {
        return templateService.downloadTemplate(tenantId, configKey);
    }

    private void archiveDefinition(TenantOnboardingDocumentDefinition definition) {
        Instant now = Instant.now();
        LifecycleFields.archiveCatalogEntry(definition, now);
        definitionRepository.save(definition);
        log.info("Archived tenant onboarding document '{}'", definition.getConfigKey());
    }

    private void unarchiveDefinition(TenantOnboardingDocumentDefinition definition) {
        LifecycleFields.unarchiveCatalogEntry(
                definition, templateService.hasCurrentTemplate(definition));
        definitionRepository.save(definition);
        log.info("Unarchived tenant onboarding document '{}'", definition.getConfigKey());
    }

    private boolean isDeletable(TenantOnboardingDocumentDefinition definition) {
        String configKey = definition.getConfigKey();
        return !fulfillmentRepository.existsByConfigKeyAndOrganizationIdIsNull(configKey)
                && !fulfillmentEventRepository.existsByConfigKeyAndOrganizationIdIsNull(configKey);
    }

    private boolean isArchivable(TenantOnboardingDocumentDefinition definition) {
        return !definition.isArchived() && !isDeletable(definition);
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

    private static String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }

    private void applyRequestedActive(
            TenantOnboardingDocumentDefinition definition, boolean requestedActive) {
        if (requestedActive && !templateService.hasCurrentTemplate(definition)) {
            throw new ConflictException(
                    "Onboarding document cannot be active without a template file");
        }
        definition.setActive(requestedActive);
    }

    private TenantOnboardingDocumentDefinitionDto toDto(
            TenantOnboardingDocumentDefinition definition) {
        var templateMetadata = templateService.findCurrentTemplateMetadata(definition);
        return TenantOnboardingDocumentDefinitionDto.builder()
                .id(definition.getId())
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

    private <T> T withTenantContext(long tenantId, Supplier<T> action) {
        TenantScopeGuard.bind(tenantId);
        TenantScopeGuard.requireActiveTenantMatches(tenantId);
        return action.get();
    }

    private void retargetConfigKey(String oldKey, String newKey) {
        if (oldKey.equals(newKey)) {
            return;
        }
        standardOnboardingStepService.renameDocumentConfigKey(oldKey, newKey);
    }
}
