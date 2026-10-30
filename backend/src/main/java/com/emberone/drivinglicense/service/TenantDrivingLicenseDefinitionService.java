/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.drivinglicense.service;

import com.emberone.drivinglicense.dto.CreateTenantDrivingLicenseDefinitionRequest;
import com.emberone.drivinglicense.dto.TenantDrivingLicenseDefinitionDto;
import com.emberone.drivinglicense.dto.UpdateTenantDrivingLicenseDefinitionRequest;
import com.emberone.drivinglicense.event.TenantDrivingLicenseDefinitionDeletedEvent;
import com.emberone.drivinglicense.repository.UserDrivingLicenseRepository;
import com.emberone.drivinglicense.standard.DrivingLicenseDefinitionSelection;
import com.emberone.drivinglicense.standard.DrivingLicenseDefinitionSelectionRepository;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinition;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinitionRepository;
import com.emberone.drivinglicense.util.DrivingLicenseAbbreviation;
import com.emberone.federalstate.baseline.DrivingLicenseDefinitionBaseline;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.qualification.tenant.TenantQualificationDrivingLicense;
import com.emberone.qualification.tenant.TenantQualificationDrivingLicenseRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantDrivingLicenseDefinitionService {

    private final TenantDrivingLicenseDefinitionRepository tenantDefinitionRepository;
    private final DrivingLicenseDefinitionSelectionRepository selectionRepository;
    private final UserDrivingLicenseRepository drivingLicenseRepository;
    private final TenantQualificationDrivingLicenseRepository qualificationDrivingLicenseRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<TenantDrivingLicenseDefinitionDto> list(long tenantId) {
        return withTenantContext(
                tenantId,
                () ->
                        tenantDefinitionRepository.findAllByOrderByNameAsc().stream()
                                .map(this::toDto)
                                .toList());
    }

    @Transactional(readOnly = true)
    public TenantDrivingLicenseDefinitionDto get(long tenantId, String configKey) {
        return withTenantContext(tenantId, () -> toDto(requireDefinition(configKey)));
    }

    @Transactional
    public TenantDrivingLicenseDefinitionDto create(
            long tenantId, CreateTenantDrivingLicenseDefinitionRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    String abbreviation =
                            DrivingLicenseAbbreviation.requireValid(request.getAbbreviation());
                    if (tenantDefinitionRepository.existsByConfigKeyAndArchivedFalse(
                            abbreviation)) {
                        throw new ConflictException(
                                "Driving license definition abbreviation already exists: "
                                        + abbreviation);
                    }
                    TenantDrivingLicenseDefinition saved =
                            tenantDefinitionRepository.save(
                                    TenantDrivingLicenseDefinition.builder()
                                            .tenantId(tenantId)
                                            .configKey(abbreviation)
                                            .name(request.getName().trim())
                                            .active(request.isActive())
                                            .build());
                    log.info("Created tenant driving license definition '{}'", abbreviation);
                    return toDto(saved);
                });
    }

    @Transactional
    public void seedBaselineDefinitionsIfMissing(
            long tenantId, List<DrivingLicenseDefinitionBaseline> items) {
        withTenantContext(
                tenantId,
                () -> {
                    for (DrivingLicenseDefinitionBaseline item : items) {
                        if (tenantDefinitionRepository
                                .findByConfigKey(item.configKey())
                                .isPresent()) {
                            continue;
                        }
                        tenantDefinitionRepository.save(
                                TenantDrivingLicenseDefinition.builder()
                                        .tenantId(tenantId)
                                        .configKey(item.configKey())
                                        .name(item.name())
                                        .active(item.active())
                                        .build());
                    }
                    return null;
                });
    }

    @Transactional
    public TenantDrivingLicenseDefinitionDto update(
            long tenantId, String configKey, UpdateTenantDrivingLicenseDefinitionRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    TenantDrivingLicenseDefinition definition = requireActiveDefinition(configKey);
                    String abbreviation =
                            DrivingLicenseAbbreviation.requireValid(request.getAbbreviation());
                    if (tenantDefinitionRepository.existsByConfigKeyAndArchivedFalseAndIdNot(
                            abbreviation, definition.getId())) {
                        throw new ConflictException(
                                "Driving license definition abbreviation already exists: "
                                        + abbreviation);
                    }
                    retargetConfigKey(definition.getConfigKey(), abbreviation);
                    definition.setConfigKey(abbreviation);
                    definition.setName(request.getName().trim());
                    definition.setActive(request.isActive());
                    TenantDrivingLicenseDefinition saved =
                            tenantDefinitionRepository.save(definition);
                    log.info("Updated tenant driving license definition '{}'", configKey);
                    return toDto(saved);
                });
    }

    @Transactional
    public void delete(long tenantId, String configKey) {
        withTenantContext(
                tenantId,
                () -> {
                    TenantDrivingLicenseDefinition definition = requireDefinition(configKey);
                    if (!isDeletable(definition)) {
                        throw new ConflictException(
                                "Driving license definition is in use and cannot be deleted");
                    }
                    selectionRepository.deleteByDefinitionConfigKey(configKey);
                    eventPublisher.publishEvent(
                            new TenantDrivingLicenseDefinitionDeletedEvent(tenantId, configKey));
                    tenantDefinitionRepository.delete(definition);
                    log.info("Deleted tenant driving license definition '{}'", configKey);
                    return null;
                });
    }

    @Transactional
    public void archive(long tenantId, String configKey) {
        withTenantContext(
                tenantId,
                () -> {
                    TenantDrivingLicenseDefinition definition = requireActiveDefinition(configKey);
                    if (!isArchivable(definition)) {
                        throw new ConflictException(
                                "Driving license definition cannot be archived");
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
                    TenantDrivingLicenseDefinition definition = requireDefinition(configKey);
                    if (!definition.isArchived()) {
                        throw new ConflictException("Driving license definition is not archived");
                    }
                    if (tenantDefinitionRepository.existsByConfigKeyAndArchivedFalseAndIdNot(
                            definition.getConfigKey(), definition.getId())) {
                        throw new ConflictException(
                                "Driving license definition abbreviation already exists: "
                                        + definition.getConfigKey());
                    }
                    unarchiveDefinition(definition);
                    return null;
                });
    }

    @Transactional(readOnly = true)
    public List<TenantDrivingLicenseDefinition> findActiveByConfigKeys(List<String> configKeys) {
        return tenantDefinitionRepository.findByConfigKeyIn(configKeys).stream()
                .filter(definition -> !definition.isArchived())
                .sorted(
                        Comparator.comparing(
                                TenantDrivingLicenseDefinition::getName,
                                String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void archiveDefinition(TenantDrivingLicenseDefinition definition) {
        Instant now = Instant.now();
        LifecycleFields.archiveCatalogEntry(definition, now);
        tenantDefinitionRepository.save(definition);
        log.info("Archived tenant driving license definition '{}'", definition.getConfigKey());
    }

    private void unarchiveDefinition(TenantDrivingLicenseDefinition definition) {
        LifecycleFields.unarchiveCatalogEntry(definition, true);
        tenantDefinitionRepository.save(definition);
        log.info("Unarchived tenant driving license definition '{}'", definition.getConfigKey());
    }

    private boolean hasRecords(TenantDrivingLicenseDefinition definition) {
        return drivingLicenseRepository.existsByClasses_TenantDrivingLicenseDefinitionId(
                definition.getId());
    }

    private boolean isDeletable(TenantDrivingLicenseDefinition definition) {
        return !hasRecords(definition);
    }

    private boolean isArchivable(TenantDrivingLicenseDefinition definition) {
        return !definition.isArchived() && hasRecords(definition);
    }

    private TenantDrivingLicenseDefinition requireDefinition(String configKey) {
        return tenantDefinitionRepository
                .findByConfigKey(configKey)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Tenant driving license definition not found: "
                                                + configKey));
    }

    private TenantDrivingLicenseDefinition requireActiveDefinition(String configKey) {
        TenantDrivingLicenseDefinition definition = requireDefinition(configKey);
        if (definition.isArchived()) {
            throw new ResourceNotFoundException(
                    "Tenant driving license definition not found: " + configKey);
        }
        return definition;
    }

    private TenantDrivingLicenseDefinitionDto toDto(TenantDrivingLicenseDefinition definition) {
        return TenantDrivingLicenseDefinitionDto.builder()
                .id(definition.getId())
                .configKey(definition.getConfigKey())
                .abbreviation(definition.getConfigKey())
                .name(definition.getName())
                .active(definition.isActive())
                .archived(definition.isArchived())
                .deletable(isDeletable(definition))
                .archivable(isArchivable(definition))
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
        for (DrivingLicenseDefinitionSelection selection :
                selectionRepository.findByDefinitionConfigKey(oldKey)) {
            selection.setDefinitionConfigKey(newKey);
        }
        for (TenantQualificationDrivingLicense link :
                qualificationDrivingLicenseRepository.findByDrivingLicenseConfigKey(oldKey)) {
            link.setDrivingLicenseConfigKey(newKey);
        }
    }
}
