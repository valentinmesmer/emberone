/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.standard;

import com.emberone.platform.exception.ConflictException;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.spi.StandardContentItem;
import com.emberone.standard.spi.StandardDomain;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(51)
@RequiredArgsConstructor
public class TrainingCourseStandardDomain implements StandardDomain {

    private final TrainingCourseTypeSelectionRepository selectionRepository;
    private final TenantTrainingCourseTypeRepository tenantTypeRepository;

    @Override
    public String domainKey() {
        return "training-courses";
    }

    @Override
    @Transactional
    public void copyTemplates(Standard source, Standard target) {
        for (TrainingCourseTypeSelection selection :
                selectionRepository.findByStandardId(source.getId())) {
            if (selectionRepository.existsByStandardIdAndDefinitionConfigKey(
                    target.getId(), selection.getDefinitionConfigKey())) {
                continue;
            }
            selectionRepository.save(
                    TrainingCourseTypeSelection.builder()
                            .tenantId(target.getTenantId())
                            .standardId(target.getId())
                            .definitionConfigKey(selection.getDefinitionConfigKey())
                            .build());
        }
    }

    @Override
    @Transactional
    public void materializeForOrg(Long organizationId, Standard standard) {
        // Training courses are tenant-scoped; standard selections resolve at catalog read time.
    }

    @Override
    @Transactional
    public void reassignForOrg(Long organizationId, Standard standard) {
        // Training courses are tenant-scoped; standard selections resolve at catalog read time.
    }

    @Override
    @Transactional(readOnly = true)
    public List<StandardContentItem> describeContent(Standard standard) {
        List<StandardContentItem> items = new ArrayList<>();
        for (TrainingCourseTypeSelection selection :
                selectionRepository.findByStandardId(standard.getId())) {
            tenantTypeRepository
                    .findByConfigKey(selection.getDefinitionConfigKey())
                    .filter(type -> !type.isArchived())
                    .ifPresent(
                            type ->
                                    items.add(
                                            new StandardContentItem(
                                                    domainKey(),
                                                    type.getConfigKey(),
                                                    type.getName(),
                                                    type.getConfigKey(),
                                                    List.of(),
                                                    Map.of(
                                                            "active",
                                                            String.valueOf(type.isActive())),
                                                    false)));
        }
        items.sort(Comparator.comparing(StandardContentItem::configKey));
        return items;
    }

    @Transactional
    public void selectAllActiveDefinitions(Standard standard) {
        List<String> keys =
                tenantTypeRepository.findAllByOrderByNameAsc().stream()
                        .filter(type -> !type.isArchived())
                        .map(TenantTrainingCourseType::getConfigKey)
                        .toList();
        replaceSelections(standard, keys);
    }

    @Transactional
    public void replaceSelections(Standard standard, List<String> definitionConfigKeys) {
        Long tenantId = standard.getTenantId();
        Long standardId = standard.getId();
        Set<String> uniqueKeys = new LinkedHashSet<>();
        for (String configKey : definitionConfigKeys) {
            if (configKey == null || configKey.isBlank()) {
                continue;
            }
            String trimmed = configKey.trim();
            TenantTrainingCourseType tenantType =
                    tenantTypeRepository
                            .findByConfigKey(trimmed)
                            .orElseThrow(
                                    () ->
                                            new ConflictException(
                                                    "Unknown tenant training course type: "
                                                            + trimmed));
            if (tenantType.isArchived()) {
                throw new ConflictException("Tenant training course type is archived: " + trimmed);
            }
            uniqueKeys.add(trimmed);
        }

        Set<String> existingKeys =
                selectionRepository.findByStandardId(standardId).stream()
                        .map(TrainingCourseTypeSelection::getDefinitionConfigKey)
                        .collect(Collectors.toSet());

        for (String key : uniqueKeys) {
            if (!existingKeys.contains(key)) {
                selectionRepository.save(
                        TrainingCourseTypeSelection.builder()
                                .tenantId(tenantId)
                                .standardId(standardId)
                                .definitionConfigKey(key)
                                .build());
            }
        }
        for (TrainingCourseTypeSelection selection :
                selectionRepository.findByStandardId(standardId)) {
            if (!uniqueKeys.contains(selection.getDefinitionConfigKey())) {
                selectionRepository.delete(selection);
            }
        }
    }
}
