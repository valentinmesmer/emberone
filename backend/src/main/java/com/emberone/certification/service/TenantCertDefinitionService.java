/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.service;

import com.emberone.certification.dto.CreateTenantCertDefinitionRequest;
import com.emberone.certification.dto.TenantCertDefinitionDto;
import com.emberone.certification.dto.UpdateTenantCertDefinitionRequest;
import com.emberone.certification.event.CertificationEventGrantRepository;
import com.emberone.certification.event.TenantCertDefinitionArchivedEvent;
import com.emberone.certification.event.TenantCertDefinitionDeletedEvent;
import com.emberone.certification.repository.UserCertificationAssignmentRepository;
import com.emberone.certification.repository.UserCertificationRecordRepository;
import com.emberone.certification.standard.CertDefinitionSelection;
import com.emberone.certification.standard.CertDefinitionSelectionRepository;
import com.emberone.certification.tenant.TenantCertDefinition;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.certification.util.CertificationAbbreviation;
import com.emberone.federalstate.baseline.CertificationDefinitionBaseline;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.qualification.tenant.TenantQualificationCertDefinition;
import com.emberone.qualification.tenant.TenantQualificationCertDefinitionRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantCertDefinitionService {

    private final TenantCertDefinitionRepository tenantDefinitionRepository;
    private final CertDefinitionSelectionRepository selectionRepository;
    private final UserCertificationAssignmentRepository assignmentRepository;
    private final UserCertificationRecordRepository recordRepository;
    private final CertificationEventGrantRepository eventGrantRepository;
    private final TenantQualificationCertDefinitionRepository qualificationCertDefinitionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TenantCertDefinitionEvidenceTypeService evidenceTypeService;

    private static String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    @Transactional(readOnly = true)
    public List<TenantCertDefinitionDto> list(long tenantId) {
        return withTenantContext(
                tenantId,
                () ->
                        tenantDefinitionRepository.findAllByOrderByNameAsc().stream()
                                .map(this::toDto)
                                .toList());
    }

    @Transactional(readOnly = true)
    public TenantCertDefinitionDto get(long tenantId, String configKey) {
        return withTenantContext(tenantId, () -> toDto(requireDefinition(configKey)));
    }

    @Transactional
    public TenantCertDefinitionDto create(
            long tenantId, CreateTenantCertDefinitionRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    String abbreviation =
                            CertificationAbbreviation.requireValid(request.getAbbreviation());
                    if (tenantDefinitionRepository.existsByConfigKeyAndArchivedFalse(
                            abbreviation)) {
                        throw new ConflictException(
                                "Certification definition abbreviation already exists: "
                                        + abbreviation);
                    }
                    TenantCertDefinition saved =
                            tenantDefinitionRepository.save(
                                    TenantCertDefinition.builder()
                                            .tenantId(tenantId)
                                            .configKey(abbreviation)
                                            .name(request.getName().trim())
                                            .description(normalize(request.getDescription()))
                                            .intervalDays(request.getIntervalDays())
                                            .reminderDaysBeforeExpiry(
                                                    request.getReminderDaysBeforeExpiry() == null
                                                            ? 30
                                                            : request.getReminderDaysBeforeExpiry())
                                            .active(request.isActive())
                                            .build());
                    evidenceTypeService.replaceEvidenceTypes(
                            tenantId, saved.getId(), request.getEvidenceTypes());
                    log.info("Created tenant certification definition '{}'", abbreviation);
                    return toDto(saved);
                });
    }

    @Transactional
    public TenantCertDefinitionDto update(
            long tenantId, String configKey, UpdateTenantCertDefinitionRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    TenantCertDefinition definition = requireActiveDefinition(configKey);
                    String abbreviation =
                            CertificationAbbreviation.requireValid(request.getAbbreviation());
                    if (tenantDefinitionRepository.existsByConfigKeyAndArchivedFalseAndIdNot(
                            abbreviation, definition.getId())) {
                        throw new ConflictException(
                                "Certification definition abbreviation already exists: "
                                        + abbreviation);
                    }
                    retargetConfigKey(definition.getConfigKey(), abbreviation);
                    definition.setConfigKey(abbreviation);
                    definition.setName(request.getName().trim());
                    definition.setDescription(normalize(request.getDescription()));
                    definition.setIntervalDays(request.getIntervalDays());
                    definition.setReminderDaysBeforeExpiry(
                            request.getReminderDaysBeforeExpiry() == null
                                    ? 30
                                    : request.getReminderDaysBeforeExpiry());
                    definition.setActive(request.isActive());
                    TenantCertDefinition saved = tenantDefinitionRepository.save(definition);
                    evidenceTypeService.replaceEvidenceTypes(
                            tenantId, saved.getId(), request.getEvidenceTypes());
                    log.info("Updated tenant certification definition '{}'", configKey);
                    return toDto(saved);
                });
    }

    @Transactional
    public void delete(long tenantId, String configKey) {
        withTenantContext(
                tenantId,
                () -> {
                    TenantCertDefinition definition = requireDefinition(configKey);
                    if (!isDeletable(definition)) {
                        throw new ConflictException(
                                "Certification definition is in use and cannot be deleted");
                    }
                    selectionRepository.deleteByDefinitionConfigKey(configKey);
                    eventPublisher.publishEvent(
                            new TenantCertDefinitionDeletedEvent(tenantId, configKey));
                    tenantDefinitionRepository.delete(definition);
                    log.info("Deleted tenant certification definition '{}'", configKey);
                    return null;
                });
    }

    @Transactional
    public void archive(long tenantId, String configKey) {
        withTenantContext(
                tenantId,
                () -> {
                    TenantCertDefinition definition = requireActiveDefinition(configKey);
                    if (!isArchivable(definition)) {
                        throw new ConflictException("Certification definition cannot be archived");
                    }
                    selectionRepository.deleteByDefinitionConfigKey(configKey);
                    archiveDefinition(definition);
                    return null;
                });
    }

    @Transactional
    public void unarchive(long tenantId, String configKey) {
        withTenantContext(
                tenantId,
                () -> {
                    TenantCertDefinition definition = requireDefinition(configKey);
                    if (!definition.isArchived()) {
                        throw new ConflictException("Certification definition is not archived");
                    }
                    if (tenantDefinitionRepository.existsByConfigKeyAndArchivedFalseAndIdNot(
                            definition.getConfigKey(), definition.getId())) {
                        throw new ConflictException(
                                "Certification definition abbreviation already exists: "
                                        + definition.getConfigKey());
                    }
                    unarchiveDefinition(definition);
                    return null;
                });
    }

    /**
     * Inserts baseline definitions for a tenant when missing. Existing rows are left unchanged so
     * later baseline updates do not affect tenants that were already provisioned.
     */
    @Transactional
    public void seedBaselineDefinitionsIfMissing(
            long tenantId, List<CertificationDefinitionBaseline> items) {
        withTenantContext(
                tenantId,
                () -> {
                    for (CertificationDefinitionBaseline item : items) {
                        if (tenantDefinitionRepository
                                .findByConfigKey(item.configKey())
                                .isPresent()) {
                            continue;
                        }
                        TenantCertDefinition definition =
                                TenantCertDefinition.builder()
                                        .tenantId(tenantId)
                                        .configKey(item.configKey())
                                        .build();
                        applyBaseline(definition, item);
                        tenantDefinitionRepository.save(definition);
                        evidenceTypeService.seedDefaultDocumentEvidenceType(
                                tenantId, definition.getId());
                    }
                    return null;
                });
    }

    private void applyBaseline(
            TenantCertDefinition definition, CertificationDefinitionBaseline item) {
        definition.setName(item.name());
        definition.setDescription(normalize(item.description()));
        definition.setIntervalDays(item.intervalDays());
        definition.setReminderDaysBeforeExpiry(
                item.reminderDaysBeforeExpiry() == null ? 30 : item.reminderDaysBeforeExpiry());
        definition.setActive(item.active());
    }

    @Transactional(readOnly = true)
    public List<TenantCertDefinition> findActiveByConfigKeys(List<String> configKeys) {
        return tenantDefinitionRepository.findByConfigKeyIn(configKeys).stream()
                .filter(definition -> !definition.isArchived())
                .sorted(
                        Comparator.comparing(
                                TenantCertDefinition::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void archiveDefinition(TenantCertDefinition definition) {
        Instant now = Instant.now();
        LifecycleFields.archiveCatalogEntry(definition, now);
        tenantDefinitionRepository.save(definition);
        eventPublisher.publishEvent(
                new TenantCertDefinitionArchivedEvent(
                        definition.getTenantId(), definition.getId()));
        log.info("Archived tenant certification definition '{}'", definition.getConfigKey());
    }

    private void unarchiveDefinition(TenantCertDefinition definition) {
        LifecycleFields.unarchiveCatalogEntry(definition, true);
        tenantDefinitionRepository.save(definition);
        log.info("Unarchived tenant certification definition '{}'", definition.getConfigKey());
    }

    private boolean hasRecords(TenantCertDefinition definition) {
        return recordRepository.existsByTenantCertDefinitionIdIn(List.of(definition.getId()));
    }

    private boolean hasAssignments(TenantCertDefinition definition) {
        return assignmentRepository.existsByTenantCertDefinitionIdIn(List.of(definition.getId()));
    }

    private boolean hasEventGrants(TenantCertDefinition definition) {
        return eventGrantRepository.existsByTenantCertDefinitionId(definition.getId());
    }

    private boolean isDeletable(TenantCertDefinition definition) {
        return !hasRecords(definition)
                && !hasAssignments(definition)
                && !hasEventGrants(definition);
    }

    private boolean isArchivable(TenantCertDefinition definition) {
        return !definition.isArchived() && (hasRecords(definition) || hasAssignments(definition));
    }

    private TenantCertDefinition requireDefinition(String configKey) {
        return tenantDefinitionRepository
                .findByConfigKey(configKey)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Tenant certification definition not found: " + configKey));
    }

    private TenantCertDefinition requireActiveDefinition(String configKey) {
        TenantCertDefinition definition = requireDefinition(configKey);
        if (definition.isArchived()) {
            throw new ResourceNotFoundException(
                    "Tenant certification definition not found: " + configKey);
        }
        return definition;
    }

    private TenantCertDefinitionDto toDto(TenantCertDefinition definition) {
        Set<String> evidenceTypes =
                TenantCertDefinitionEvidenceTypeService.toStringSet(
                        evidenceTypeService.getEvidenceTypes(definition.getId()));
        return TenantCertDefinitionDto.builder()
                .id(definition.getId())
                .configKey(definition.getConfigKey())
                .abbreviation(definition.getConfigKey())
                .name(definition.getName())
                .description(definition.getDescription())
                .intervalDays(definition.getIntervalDays())
                .reminderDaysBeforeExpiry(definition.getReminderDaysBeforeExpiry())
                .active(definition.isActive())
                .archived(definition.isArchived())
                .deletable(isDeletable(definition))
                .archivable(isArchivable(definition))
                .evidenceTypes(evidenceTypes)
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
        for (CertDefinitionSelection selection :
                selectionRepository.findByDefinitionConfigKey(oldKey)) {
            selection.setDefinitionConfigKey(newKey);
        }
        for (TenantQualificationCertDefinition link :
                qualificationCertDefinitionRepository.findByCertDefinitionConfigKey(oldKey)) {
            link.setCertDefinitionConfigKey(newKey);
        }
    }
}
