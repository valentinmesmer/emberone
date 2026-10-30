/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.service;

import com.emberone.certification.tenant.TenantCertDefinition;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinition;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinitionRepository;
import com.emberone.federalstate.baseline.QualificationDefinitionBaseline;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.qualification.dto.CreateTenantQualificationDefinitionRequest;
import com.emberone.qualification.dto.QualificationDefinitionDto;
import com.emberone.qualification.dto.TenantQualificationDefinitionDto;
import com.emberone.qualification.dto.UpdateTenantQualificationDefinitionRequest;
import com.emberone.qualification.event.QualificationDefinitionChangedEvent;
import com.emberone.qualification.repository.UserQualificationOrgDecisionRepository;
import com.emberone.qualification.standard.QualificationSelection;
import com.emberone.qualification.standard.QualificationSelectionRepository;
import com.emberone.qualification.tenant.*;
import com.emberone.qualification.util.QualificationAbbreviation;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantQualificationDefinitionService {

    private final TenantQualificationDefinitionRepository definitionRepository;
    private final QualificationSelectionRepository selectionRepository;
    private final UserQualificationOrgDecisionRepository orgDecisionRepository;
    private final TenantTrainingCourseTypeRepository trainingCourseTypeRepository;
    private final TenantDrivingLicenseDefinitionRepository drivingLicenseDefinitionRepository;
    private final TenantCertDefinitionRepository certDefinitionRepository;
    private final ApplicationEventPublisher eventPublisher;

    static QualificationDefinitionDto toDefinitionDto(TenantQualificationDefinition definition) {
        return QualificationDefinitionDto.builder()
                .id(definition.getId())
                .configKey(definition.getConfigKey())
                .abbreviation(definition.getConfigKey())
                .name(definition.getName())
                .description(definition.getDescription())
                .requiresOrgDecision(definition.isRequiresOrgDecision())
                .trainingCourseRequirementMode(definition.getTrainingCourseRequirementMode())
                .certRequirementMode(definition.getCertRequirementMode())
                .drivingLicenseRequirementMode(definition.getDrivingLicenseRequirementMode())
                .trainingCourseConfigKeys(
                        definition.getTrainingCourses().stream()
                                .map(TenantQualificationTrainingCourse::getTrainingCourseConfigKey)
                                .sorted()
                                .toList())
                .certDefinitionConfigKeys(
                        definition.getCertDefinitions().stream()
                                .map(TenantQualificationCertDefinition::getCertDefinitionConfigKey)
                                .sorted()
                                .toList())
                .drivingLicenseDefinitionConfigKeys(
                        definition.getDrivingLicenses().stream()
                                .map(TenantQualificationDrivingLicense::getDrivingLicenseConfigKey)
                                .sorted()
                                .toList())
                .active(definition.isActive())
                .build();
    }

    private static List<String> normalizeKeys(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return keys.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(key -> !key.isEmpty())
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private static List<String> nullSafeList(List<String> list) {
        return list == null ? List.of() : list;
    }

    @Transactional(readOnly = true)
    public List<TenantQualificationDefinitionDto> list(long tenantId) {
        return withTenantContext(
                tenantId,
                () ->
                        definitionRepository.findAllByOrderByNameAsc().stream()
                                .map(this::toDto)
                                .toList());
    }

    @Transactional
    public TenantQualificationDefinitionDto create(
            long tenantId, CreateTenantQualificationDefinitionRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    String abbreviation =
                            QualificationAbbreviation.requireValid(request.getAbbreviation());
                    if (definitionRepository.existsByConfigKeyAndArchivedFalse(abbreviation)) {
                        throw new ConflictException(
                                "Qualification definition abbreviation already exists: "
                                        + abbreviation);
                    }
                    validateRequirements(
                            request.getTrainingCourseConfigKeys(),
                            request.getCertDefinitionConfigKeys(),
                            request.getDrivingLicenseDefinitionConfigKeys(),
                            request.isRequiresOrgDecision());
                    TenantQualificationDefinition definition =
                            TenantQualificationDefinition.builder()
                                    .tenantId(tenantId)
                                    .configKey(abbreviation)
                                    .name(request.getName().trim())
                                    .description(normalize(request.getDescription()))
                                    .requiresOrgDecision(request.isRequiresOrgDecision())
                                    .trainingCourseRequirementMode(
                                            request.getTrainingCourseRequirementMode())
                                    .certRequirementMode(request.getCertRequirementMode())
                                    .drivingLicenseRequirementMode(
                                            request.getDrivingLicenseRequirementMode())
                                    .active(request.isActive())
                                    .build();
                    applyRequirements(
                            definition,
                            tenantId,
                            request.getTrainingCourseConfigKeys(),
                            request.getCertDefinitionConfigKeys(),
                            request.getDrivingLicenseDefinitionConfigKeys());
                    TenantQualificationDefinition saved = definitionRepository.save(definition);
                    log.info("Created tenant qualification definition '{}'", abbreviation);
                    eventPublisher.publishEvent(
                            new QualificationDefinitionChangedEvent(tenantId, saved.getId()));
                    return toDto(saved);
                });
    }

    /**
     * Inserts baseline qualification definitions when missing. Existing non-archived rows receive
     * any newly added baseline requirements (additive; tenant customizations are not removed).
     */
    @Transactional
    public void seedBaselineDefinitionsIfMissing(
            long tenantId, List<QualificationDefinitionBaseline> items) {
        withTenantContext(
                tenantId,
                () -> {
                    for (QualificationDefinitionBaseline item : items) {
                        List<String> trainingKeys = nullSafeList(item.trainingCourseConfigKeys());
                        List<String> certKeys = nullSafeList(item.certDefinitionConfigKeys());
                        List<String> drivingLicenseKeys =
                                nullSafeList(item.drivingLicenseDefinitionConfigKeys());
                        Optional<TenantQualificationDefinition> existing =
                                definitionRepository.findByConfigKey(item.configKey());
                        if (existing.isPresent()) {
                            TenantQualificationDefinition definition = existing.get();
                            if (definition.isArchived()) {
                                continue;
                            }
                            if (addMissingRequirements(
                                    definition,
                                    tenantId,
                                    trainingKeys,
                                    certKeys,
                                    drivingLicenseKeys)) {
                                definitionRepository.save(definition);
                                eventPublisher.publishEvent(
                                        new QualificationDefinitionChangedEvent(
                                                tenantId, definition.getId()));
                            }
                            continue;
                        }
                        validateRequirements(
                                trainingKeys,
                                certKeys,
                                drivingLicenseKeys,
                                item.requiresOrgDecision());
                        TenantQualificationDefinition definition =
                                TenantQualificationDefinition.builder()
                                        .tenantId(tenantId)
                                        .configKey(item.configKey())
                                        .name(item.name().trim())
                                        .description(normalize(item.description()))
                                        .requiresOrgDecision(item.requiresOrgDecision())
                                        .trainingCourseRequirementMode(
                                                item.trainingCourseRequirementMode())
                                        .certRequirementMode(item.certRequirementMode())
                                        .drivingLicenseRequirementMode(
                                                item.drivingLicenseRequirementMode())
                                        .active(item.active())
                                        .build();
                        applyRequirements(
                                definition, tenantId, trainingKeys, certKeys, drivingLicenseKeys);
                        definitionRepository.save(definition);
                    }
                    return null;
                });
    }

    @Transactional
    public TenantQualificationDefinitionDto update(
            long tenantId, String configKey, UpdateTenantQualificationDefinitionRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    TenantQualificationDefinition definition = requireActiveDefinition(configKey);
                    String abbreviation =
                            QualificationAbbreviation.requireValid(request.getAbbreviation());
                    if (definitionRepository.existsByConfigKeyAndArchivedFalseAndIdNot(
                            abbreviation, definition.getId())) {
                        throw new ConflictException(
                                "Qualification definition abbreviation already exists: "
                                        + abbreviation);
                    }
                    validateRequirements(
                            request.getTrainingCourseConfigKeys(),
                            request.getCertDefinitionConfigKeys(),
                            request.getDrivingLicenseDefinitionConfigKeys(),
                            request.isRequiresOrgDecision());
                    retargetConfigKey(definition.getConfigKey(), abbreviation);
                    definition.setConfigKey(abbreviation);
                    definition.setName(request.getName().trim());
                    definition.setDescription(normalize(request.getDescription()));
                    definition.setRequiresOrgDecision(request.isRequiresOrgDecision());
                    definition.setTrainingCourseRequirementMode(
                            request.getTrainingCourseRequirementMode());
                    definition.setCertRequirementMode(request.getCertRequirementMode());
                    definition.setDrivingLicenseRequirementMode(
                            request.getDrivingLicenseRequirementMode());
                    definition.setActive(request.isActive());
                    replaceRequirements(
                            definition,
                            tenantId,
                            request.getTrainingCourseConfigKeys(),
                            request.getCertDefinitionConfigKeys(),
                            request.getDrivingLicenseDefinitionConfigKeys());
                    TenantQualificationDefinition saved = definitionRepository.save(definition);
                    log.info("Updated tenant qualification definition '{}'", configKey);
                    eventPublisher.publishEvent(
                            new QualificationDefinitionChangedEvent(tenantId, saved.getId()));
                    return toDto(saved);
                });
    }

    @Transactional
    public void delete(long tenantId, String configKey) {
        withTenantContext(
                tenantId,
                () -> {
                    TenantQualificationDefinition definition = requireDefinition(configKey);
                    if (!isDeletable(definition)) {
                        throw new ConflictException(
                                "Qualification definition is in use and cannot be deleted");
                    }
                    selectionRepository.deleteByDefinitionConfigKey(configKey);
                    definitionRepository.delete(definition);
                    log.info("Deleted tenant qualification definition '{}'", configKey);
                    return null;
                });
    }

    @Transactional
    public void archive(long tenantId, String configKey) {
        withTenantContext(
                tenantId,
                () -> {
                    TenantQualificationDefinition definition = requireActiveDefinition(configKey);
                    if (!isArchivable(definition)) {
                        throw new ConflictException("Qualification definition cannot be archived");
                    }
                    selectionRepository.deleteByDefinitionConfigKey(configKey);
                    archiveDefinition(definition);
                    eventPublisher.publishEvent(
                            new QualificationDefinitionChangedEvent(tenantId, definition.getId()));
                    return null;
                });
    }

    @Transactional
    public void unarchive(long tenantId, String configKey) {
        withTenantContext(
                tenantId,
                () -> {
                    TenantQualificationDefinition definition = requireDefinition(configKey);
                    if (!definition.isArchived()) {
                        throw new ConflictException("Qualification definition is not archived");
                    }
                    if (definitionRepository.existsByConfigKeyAndArchivedFalseAndIdNot(
                            definition.getConfigKey(), definition.getId())) {
                        throw new ConflictException(
                                "Qualification definition abbreviation already exists: "
                                        + definition.getConfigKey());
                    }
                    unarchiveDefinition(definition);
                    eventPublisher.publishEvent(
                            new QualificationDefinitionChangedEvent(tenantId, definition.getId()));
                    return null;
                });
    }

    @Transactional(readOnly = true)
    public TenantQualificationDefinition requireActiveDefinition(String configKey) {
        return requireActiveDefinitionInternal(configKey);
    }

    private void validateRequirements(
            List<String> trainingKeys,
            List<String> certKeys,
            List<String> drivingLicenseKeys,
            boolean requiresOrgDecision) {
        List<String> training = normalizeKeys(trainingKeys);
        List<String> certs = normalizeKeys(certKeys);
        List<String> drivingLicenses = normalizeKeys(drivingLicenseKeys);
        if (training.isEmpty()
                && certs.isEmpty()
                && drivingLicenses.isEmpty()
                && !requiresOrgDecision) {
            throw new ConflictException(
                    "At least one training course, certification, or driving license requirement is required, "
                            + "or an org decision must be required");
        }
        for (String key : training) {
            requireActiveTrainingType(key);
        }
        for (String key : certs) {
            requireActiveCertDefinition(key);
        }
        for (String key : drivingLicenses) {
            requireActiveDrivingLicenseDefinition(key);
        }
    }

    /**
     * Adds baseline requirement keys that are not yet linked. Does not remove tenant-added keys.
     *
     * @return {@code true} if at least one requirement was added
     */
    private boolean addMissingRequirements(
            TenantQualificationDefinition definition,
            long tenantId,
            List<String> trainingKeys,
            List<String> certKeys,
            List<String> drivingLicenseKeys) {
        boolean added = false;
        Set<String> presentTraining =
                definition.getTrainingCourses().stream()
                        .map(TenantQualificationTrainingCourse::getTrainingCourseConfigKey)
                        .collect(Collectors.toSet());
        for (String key : normalizeKeys(trainingKeys)) {
            if (presentTraining.add(key)) {
                requireActiveTrainingType(key);
                definition
                        .getTrainingCourses()
                        .add(
                                TenantQualificationTrainingCourse.builder()
                                        .tenantId(tenantId)
                                        .qualificationDefinition(definition)
                                        .trainingCourseConfigKey(key)
                                        .build());
                added = true;
            }
        }
        Set<String> presentCerts =
                definition.getCertDefinitions().stream()
                        .map(TenantQualificationCertDefinition::getCertDefinitionConfigKey)
                        .collect(Collectors.toSet());
        for (String key : normalizeKeys(certKeys)) {
            if (presentCerts.add(key)) {
                requireActiveCertDefinition(key);
                definition
                        .getCertDefinitions()
                        .add(
                                TenantQualificationCertDefinition.builder()
                                        .tenantId(tenantId)
                                        .qualificationDefinition(definition)
                                        .certDefinitionConfigKey(key)
                                        .build());
                added = true;
            }
        }
        Set<String> presentDrivingLicenses =
                definition.getDrivingLicenses().stream()
                        .map(TenantQualificationDrivingLicense::getDrivingLicenseConfigKey)
                        .collect(Collectors.toSet());
        for (String key : normalizeKeys(drivingLicenseKeys)) {
            if (presentDrivingLicenses.add(key)) {
                requireActiveDrivingLicenseDefinition(key);
                definition
                        .getDrivingLicenses()
                        .add(
                                TenantQualificationDrivingLicense.builder()
                                        .tenantId(tenantId)
                                        .qualificationDefinition(definition)
                                        .drivingLicenseConfigKey(key)
                                        .build());
                added = true;
            }
        }
        return added;
    }

    private void applyRequirements(
            TenantQualificationDefinition definition,
            long tenantId,
            List<String> trainingKeys,
            List<String> certKeys,
            List<String> drivingLicenseKeys) {
        for (String key : normalizeKeys(trainingKeys)) {
            definition
                    .getTrainingCourses()
                    .add(
                            TenantQualificationTrainingCourse.builder()
                                    .tenantId(tenantId)
                                    .qualificationDefinition(definition)
                                    .trainingCourseConfigKey(key)
                                    .build());
        }
        for (String key : normalizeKeys(certKeys)) {
            definition
                    .getCertDefinitions()
                    .add(
                            TenantQualificationCertDefinition.builder()
                                    .tenantId(tenantId)
                                    .qualificationDefinition(definition)
                                    .certDefinitionConfigKey(key)
                                    .build());
        }
        for (String key : normalizeKeys(drivingLicenseKeys)) {
            definition
                    .getDrivingLicenses()
                    .add(
                            TenantQualificationDrivingLicense.builder()
                                    .tenantId(tenantId)
                                    .qualificationDefinition(definition)
                                    .drivingLicenseConfigKey(key)
                                    .build());
        }
    }

    private void replaceRequirements(
            TenantQualificationDefinition definition,
            long tenantId,
            List<String> trainingKeys,
            List<String> certKeys,
            List<String> drivingLicenseKeys) {
        syncTrainingCourses(definition, tenantId, normalizeKeys(trainingKeys));
        syncCertDefinitions(definition, tenantId, normalizeKeys(certKeys));
        syncDrivingLicenses(definition, tenantId, normalizeKeys(drivingLicenseKeys));
    }

    private void syncTrainingCourses(
            TenantQualificationDefinition definition, long tenantId, List<String> desiredKeys) {
        Set<String> desired = new LinkedHashSet<>(desiredKeys);
        definition
                .getTrainingCourses()
                .removeIf(existing -> !desired.contains(existing.getTrainingCourseConfigKey()));
        Set<String> present =
                definition.getTrainingCourses().stream()
                        .map(TenantQualificationTrainingCourse::getTrainingCourseConfigKey)
                        .collect(Collectors.toSet());
        for (String key : desired) {
            if (!present.contains(key)) {
                definition
                        .getTrainingCourses()
                        .add(
                                TenantQualificationTrainingCourse.builder()
                                        .tenantId(tenantId)
                                        .qualificationDefinition(definition)
                                        .trainingCourseConfigKey(key)
                                        .build());
            }
        }
    }

    private void syncCertDefinitions(
            TenantQualificationDefinition definition, long tenantId, List<String> desiredKeys) {
        Set<String> desired = new LinkedHashSet<>(desiredKeys);
        definition
                .getCertDefinitions()
                .removeIf(existing -> !desired.contains(existing.getCertDefinitionConfigKey()));
        Set<String> present =
                definition.getCertDefinitions().stream()
                        .map(TenantQualificationCertDefinition::getCertDefinitionConfigKey)
                        .collect(Collectors.toSet());
        for (String key : desired) {
            if (!present.contains(key)) {
                definition
                        .getCertDefinitions()
                        .add(
                                TenantQualificationCertDefinition.builder()
                                        .tenantId(tenantId)
                                        .qualificationDefinition(definition)
                                        .certDefinitionConfigKey(key)
                                        .build());
            }
        }
    }

    private void syncDrivingLicenses(
            TenantQualificationDefinition definition, long tenantId, List<String> desiredKeys) {
        Set<String> desired = new LinkedHashSet<>(desiredKeys);
        definition
                .getDrivingLicenses()
                .removeIf(existing -> !desired.contains(existing.getDrivingLicenseConfigKey()));
        Set<String> present =
                definition.getDrivingLicenses().stream()
                        .map(TenantQualificationDrivingLicense::getDrivingLicenseConfigKey)
                        .collect(Collectors.toSet());
        for (String key : desired) {
            if (!present.contains(key)) {
                definition
                        .getDrivingLicenses()
                        .add(
                                TenantQualificationDrivingLicense.builder()
                                        .tenantId(tenantId)
                                        .qualificationDefinition(definition)
                                        .drivingLicenseConfigKey(key)
                                        .build());
            }
        }
    }

    private void requireActiveTrainingType(String configKey) {
        TenantTrainingCourseType type =
                trainingCourseTypeRepository
                        .findByConfigKey(configKey)
                        .orElseThrow(
                                () ->
                                        new ConflictException(
                                                "Unknown training course type: " + configKey));
        if (type.isArchived()) {
            throw new ConflictException("Training course type is archived: " + configKey);
        }
    }

    private void requireActiveCertDefinition(String configKey) {
        TenantCertDefinition cert =
                certDefinitionRepository
                        .findByConfigKey(configKey)
                        .orElseThrow(
                                () ->
                                        new ConflictException(
                                                "Unknown certification definition: " + configKey));
        if (cert.isArchived()) {
            throw new ConflictException("Certification definition is archived: " + configKey);
        }
    }

    private void requireActiveDrivingLicenseDefinition(String configKey) {
        TenantDrivingLicenseDefinition definition =
                drivingLicenseDefinitionRepository
                        .findByConfigKey(configKey)
                        .orElseThrow(
                                () ->
                                        new ConflictException(
                                                "Unknown driving license definition: "
                                                        + configKey));
        if (definition.isArchived()) {
            throw new ConflictException("Driving license definition is archived: " + configKey);
        }
    }

    private void archiveDefinition(TenantQualificationDefinition definition) {
        Instant now = Instant.now();
        LifecycleFields.archiveCatalogEntry(definition, now);
        definitionRepository.save(definition);
        log.info("Archived tenant qualification definition '{}'", definition.getConfigKey());
    }

    private void unarchiveDefinition(TenantQualificationDefinition definition) {
        LifecycleFields.unarchiveCatalogEntry(definition, true);
        definitionRepository.save(definition);
        log.info("Unarchived tenant qualification definition '{}'", definition.getConfigKey());
    }

    private boolean hasOrgDecisions(TenantQualificationDefinition definition) {
        return orgDecisionRepository.existsByTenantQualificationDefinitionIdIn(
                List.of(definition.getId()));
    }

    private boolean isDeletable(TenantQualificationDefinition definition) {
        return !hasOrgDecisions(definition);
    }

    private boolean isArchivable(TenantQualificationDefinition definition) {
        return !definition.isArchived() && hasOrgDecisions(definition);
    }

    private TenantQualificationDefinition requireDefinition(String configKey) {
        return definitionRepository
                .findByConfigKey(configKey)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Tenant qualification definition not found: " + configKey));
    }

    private TenantQualificationDefinition requireActiveDefinitionInternal(String configKey) {
        TenantQualificationDefinition definition = requireDefinition(configKey);
        if (definition.isArchived()) {
            throw new ResourceNotFoundException(
                    "Tenant qualification definition not found: " + configKey);
        }
        return definition;
    }

    private TenantQualificationDefinitionDto toDto(TenantQualificationDefinition definition) {
        return TenantQualificationDefinitionDto.builder()
                .id(definition.getId())
                .configKey(definition.getConfigKey())
                .abbreviation(definition.getConfigKey())
                .name(definition.getName())
                .description(definition.getDescription())
                .requiresOrgDecision(definition.isRequiresOrgDecision())
                .trainingCourseRequirementMode(definition.getTrainingCourseRequirementMode())
                .certRequirementMode(definition.getCertRequirementMode())
                .drivingLicenseRequirementMode(definition.getDrivingLicenseRequirementMode())
                .trainingCourseConfigKeys(
                        definition.getTrainingCourses().stream()
                                .map(TenantQualificationTrainingCourse::getTrainingCourseConfigKey)
                                .sorted()
                                .toList())
                .certDefinitionConfigKeys(
                        definition.getCertDefinitions().stream()
                                .map(TenantQualificationCertDefinition::getCertDefinitionConfigKey)
                                .sorted()
                                .toList())
                .drivingLicenseDefinitionConfigKeys(
                        definition.getDrivingLicenses().stream()
                                .map(TenantQualificationDrivingLicense::getDrivingLicenseConfigKey)
                                .sorted()
                                .toList())
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
        for (QualificationSelection selection :
                selectionRepository.findByDefinitionConfigKey(oldKey)) {
            selection.setDefinitionConfigKey(newKey);
        }
    }
}
