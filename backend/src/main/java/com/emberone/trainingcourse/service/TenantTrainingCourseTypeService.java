/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.service;

import com.emberone.courseoffering.organization.OrganizationCourseOfferingType;
import com.emberone.courseoffering.organization.OrganizationCourseOfferingTypeRepository;
import com.emberone.courseoffering.repository.CourseOfferingRepository;
import com.emberone.federalstate.baseline.TrainingCourseTypeBaseline;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.qualification.tenant.TenantQualificationTrainingCourse;
import com.emberone.qualification.tenant.TenantQualificationTrainingCourseRepository;
import com.emberone.trainingcourse.dto.CreateTenantTrainingCourseTypeRequest;
import com.emberone.trainingcourse.dto.TenantTrainingCourseTypeDto;
import com.emberone.trainingcourse.dto.UpdateTenantTrainingCourseTypeRequest;
import com.emberone.trainingcourse.event.TenantTrainingCourseTypeDeletedEvent;
import com.emberone.trainingcourse.repository.TrainingCourseUserSubmissionRepository;
import com.emberone.trainingcourse.standard.TrainingCourseTypeSelection;
import com.emberone.trainingcourse.standard.TrainingCourseTypeSelectionRepository;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import com.emberone.trainingcourse.util.TrainingCourseAbbreviation;
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
public class TenantTrainingCourseTypeService {

    private final TenantTrainingCourseTypeRepository tenantTypeRepository;
    private final TrainingCourseTypeSelectionRepository selectionRepository;
    private final OrganizationCourseOfferingTypeRepository courseOfferingSelectionRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final TrainingCourseUserSubmissionRepository submissionRepository;
    private final TenantQualificationTrainingCourseRepository qualificationTrainingCourseRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<TenantTrainingCourseTypeDto> list(long tenantId) {
        return withTenantContext(
                tenantId,
                () ->
                        tenantTypeRepository.findAllByOrderByNameAsc().stream()
                                .map(this::toDto)
                                .toList());
    }

    @Transactional(readOnly = true)
    public TenantTrainingCourseTypeDto get(long tenantId, String configKey) {
        return withTenantContext(tenantId, () -> toDto(requireType(configKey)));
    }

    @Transactional
    public TenantTrainingCourseTypeDto create(
            long tenantId, CreateTenantTrainingCourseTypeRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    String abbreviation =
                            TrainingCourseAbbreviation.requireValid(request.getAbbreviation());
                    if (tenantTypeRepository.existsByConfigKeyAndArchivedFalse(abbreviation)) {
                        throw new ConflictException(
                                "Training course type abbreviation already exists: "
                                        + abbreviation);
                    }
                    TenantTrainingCourseType saved =
                            tenantTypeRepository.save(
                                    TenantTrainingCourseType.builder()
                                            .tenantId(tenantId)
                                            .configKey(abbreviation)
                                            .name(request.getName().trim())
                                            .active(request.isActive())
                                            .build());
                    log.info("Created tenant training course type '{}'", abbreviation);
                    return toDto(saved);
                });
    }

    /** Inserts baseline types for a tenant when missing. Existing rows are left unchanged. */
    @Transactional
    public void seedBaselineTypesIfMissing(long tenantId, List<TrainingCourseTypeBaseline> items) {
        withTenantContext(
                tenantId,
                () -> {
                    for (TrainingCourseTypeBaseline item : items) {
                        if (tenantTypeRepository.findByConfigKey(item.configKey()).isPresent()) {
                            continue;
                        }
                        tenantTypeRepository.save(
                                TenantTrainingCourseType.builder()
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
    public TenantTrainingCourseTypeDto update(
            long tenantId, String configKey, UpdateTenantTrainingCourseTypeRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    TenantTrainingCourseType type = requireActiveType(configKey);
                    String abbreviation =
                            TrainingCourseAbbreviation.requireValid(request.getAbbreviation());
                    if (tenantTypeRepository.existsByConfigKeyAndArchivedFalseAndIdNot(
                            abbreviation, type.getId())) {
                        throw new ConflictException(
                                "Training course type abbreviation already exists: "
                                        + abbreviation);
                    }
                    retargetConfigKey(type.getConfigKey(), abbreviation);
                    type.setConfigKey(abbreviation);
                    type.setName(request.getName().trim());
                    type.setActive(request.isActive());
                    TenantTrainingCourseType saved = tenantTypeRepository.save(type);
                    log.info("Updated tenant training course type '{}'", configKey);
                    return toDto(saved);
                });
    }

    @Transactional
    public void delete(long tenantId, String configKey) {
        withTenantContext(
                tenantId,
                () -> {
                    TenantTrainingCourseType type = requireType(configKey);
                    if (!isDeletable(type)) {
                        throw new ConflictException(
                                "Training course type is in use and cannot be deleted");
                    }
                    selectionRepository.deleteByDefinitionConfigKey(configKey);
                    courseOfferingSelectionRepository.deleteByDefinitionConfigKey(configKey);
                    eventPublisher.publishEvent(
                            new TenantTrainingCourseTypeDeletedEvent(tenantId, configKey));
                    tenantTypeRepository.delete(type);
                    log.info("Deleted tenant training course type '{}'", configKey);
                    return null;
                });
    }

    @Transactional
    public void archive(long tenantId, String configKey) {
        withTenantContext(
                tenantId,
                () -> {
                    TenantTrainingCourseType type = requireActiveType(configKey);
                    if (!isArchivable(type)) {
                        throw new ConflictException("Training course type cannot be archived");
                    }
                    selectionRepository.deleteByDefinitionConfigKey(configKey);
                    courseOfferingSelectionRepository.deleteByDefinitionConfigKey(configKey);
                    archiveType(type);
                    return null;
                });
    }

    @Transactional
    public void unarchive(long tenantId, String configKey) {
        withTenantContext(
                tenantId,
                () -> {
                    TenantTrainingCourseType type = requireType(configKey);
                    if (!type.isArchived()) {
                        throw new ConflictException("Training course type is not archived");
                    }
                    if (tenantTypeRepository.existsByConfigKeyAndArchivedFalseAndIdNot(
                            type.getConfigKey(), type.getId())) {
                        throw new ConflictException(
                                "Training course type abbreviation already exists: "
                                        + type.getConfigKey());
                    }
                    unarchiveType(type);
                    return null;
                });
    }

    @Transactional(readOnly = true)
    public List<TenantTrainingCourseType> findActiveByConfigKeys(List<String> configKeys) {
        return tenantTypeRepository.findByConfigKeyIn(configKeys).stream()
                .filter(type -> !type.isArchived())
                .sorted(
                        Comparator.comparing(
                                TenantTrainingCourseType::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void archiveType(TenantTrainingCourseType type) {
        Instant now = Instant.now();
        LifecycleFields.archiveCatalogEntry(type, now);
        tenantTypeRepository.save(type);
        log.info("Archived tenant training course type '{}'", type.getConfigKey());
    }

    private void unarchiveType(TenantTrainingCourseType type) {
        LifecycleFields.unarchiveCatalogEntry(type, true);
        tenantTypeRepository.save(type);
        log.info("Unarchived tenant training course type '{}'", type.getConfigKey());
    }

    private boolean hasRecords(TenantTrainingCourseType type) {
        return submissionRepository.existsByGrantedTypes_TenantTrainingCourseTypeId(type.getId())
                || courseOfferingRepository.existsByTrainingCourseTypeId(type.getId());
    }

    private boolean isDeletable(TenantTrainingCourseType type) {
        return !hasRecords(type);
    }

    private boolean isArchivable(TenantTrainingCourseType type) {
        return !type.isArchived() && hasRecords(type);
    }

    private TenantTrainingCourseType requireType(String configKey) {
        return tenantTypeRepository
                .findByConfigKey(configKey)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Tenant training course type not found: " + configKey));
    }

    private TenantTrainingCourseType requireActiveType(String configKey) {
        TenantTrainingCourseType type = requireType(configKey);
        if (type.isArchived()) {
            throw new ResourceNotFoundException(
                    "Tenant training course type not found: " + configKey);
        }
        return type;
    }

    private TenantTrainingCourseTypeDto toDto(TenantTrainingCourseType type) {
        return TenantTrainingCourseTypeDto.builder()
                .id(type.getId())
                .configKey(type.getConfigKey())
                .abbreviation(type.getConfigKey())
                .name(type.getName())
                .active(type.isActive())
                .archived(type.isArchived())
                .deletable(isDeletable(type))
                .archivable(isArchivable(type))
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
        for (TrainingCourseTypeSelection selection :
                selectionRepository.findByDefinitionConfigKey(oldKey)) {
            selection.setDefinitionConfigKey(newKey);
        }
        for (OrganizationCourseOfferingType selection :
                courseOfferingSelectionRepository.findByDefinitionConfigKey(oldKey)) {
            selection.setDefinitionConfigKey(newKey);
        }
        for (TenantQualificationTrainingCourse link :
                qualificationTrainingCourseRepository.findByTrainingCourseConfigKey(oldKey)) {
            link.setTrainingCourseConfigKey(newKey);
        }
    }
}
