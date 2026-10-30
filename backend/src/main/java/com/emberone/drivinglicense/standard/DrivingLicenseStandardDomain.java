/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.drivinglicense.standard;

import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinition;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinitionRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.spi.StandardContentItem;
import com.emberone.standard.spi.StandardDomain;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(53)
@RequiredArgsConstructor
public class DrivingLicenseStandardDomain implements StandardDomain {

    private final DrivingLicenseDefinitionSelectionRepository selectionRepository;
    private final TenantDrivingLicenseDefinitionRepository tenantDefinitionRepository;

    @Override
    public String domainKey() {
        return "driving-licenses";
    }

    @Override
    @Transactional
    public void copyTemplates(Standard source, Standard target) {
        for (DrivingLicenseDefinitionSelection selection :
                selectionRepository.findByStandardId(source.getId())) {
            if (selectionRepository.existsByStandardIdAndDefinitionConfigKey(
                    target.getId(), selection.getDefinitionConfigKey())) {
                continue;
            }
            selectionRepository.save(
                    DrivingLicenseDefinitionSelection.builder()
                            .tenantId(target.getTenantId())
                            .standardId(target.getId())
                            .definitionConfigKey(selection.getDefinitionConfigKey())
                            .build());
        }
    }

    @Override
    @Transactional
    public void materializeForOrg(Long organizationId, Standard standard) {
        // Driving licenses are tenant-scoped; standard selections resolve at catalog read time.
    }

    @Override
    @Transactional
    public void reassignForOrg(Long organizationId, Standard standard) {
        // Driving licenses are tenant-scoped; standard selections resolve at catalog read time.
    }

    @Override
    @Transactional(readOnly = true)
    public List<StandardContentItem> describeContent(Standard standard) {
        List<StandardContentItem> items = new ArrayList<>();
        for (DrivingLicenseDefinitionSelection selection :
                selectionRepository.findByStandardId(standard.getId())) {
            tenantDefinitionRepository
                    .findByConfigKey(selection.getDefinitionConfigKey())
                    .filter(definition -> !definition.isArchived())
                    .ifPresent(
                            definition ->
                                    items.add(
                                            new StandardContentItem(
                                                    domainKey(),
                                                    definition.getConfigKey(),
                                                    definition.getName(),
                                                    definition.getConfigKey(),
                                                    List.of(),
                                                    Map.of(
                                                            "active",
                                                            String.valueOf(definition.isActive())),
                                                    false)));
        }
        items.sort(Comparator.comparing(StandardContentItem::configKey));
        return items;
    }

    @Transactional
    public void selectAllActiveDefinitions(Standard standard) {
        List<String> keys =
                tenantDefinitionRepository.findAllByOrderByNameAsc().stream()
                        .filter(definition -> !definition.isArchived())
                        .map(TenantDrivingLicenseDefinition::getConfigKey)
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
            TenantDrivingLicenseDefinition definition =
                    tenantDefinitionRepository
                            .findByConfigKey(trimmed)
                            .orElseThrow(
                                    () ->
                                            new ConflictException(
                                                    "Unknown tenant driving license definition: "
                                                            + trimmed));
            if (definition.isArchived()) {
                throw new ConflictException(
                        "Tenant driving license definition is archived: " + trimmed);
            }
            uniqueKeys.add(trimmed);
        }

        Set<String> existingKeys =
                selectionRepository.findByStandardId(standardId).stream()
                        .map(DrivingLicenseDefinitionSelection::getDefinitionConfigKey)
                        .collect(Collectors.toSet());

        for (String key : uniqueKeys) {
            if (!existingKeys.contains(key)) {
                selectionRepository.save(
                        DrivingLicenseDefinitionSelection.builder()
                                .tenantId(tenantId)
                                .standardId(standardId)
                                .definitionConfigKey(key)
                                .build());
            }
        }
        for (DrivingLicenseDefinitionSelection selection :
                selectionRepository.findByStandardId(standardId)) {
            if (!uniqueKeys.contains(selection.getDefinitionConfigKey())) {
                selectionRepository.delete(selection);
            }
        }
    }
}
