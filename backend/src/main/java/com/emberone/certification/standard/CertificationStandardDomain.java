/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.standard;

import com.emberone.certification.tenant.TenantCertDefinition;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
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
@Order(50)
@RequiredArgsConstructor
public class CertificationStandardDomain implements StandardDomain {

    private final CertDefinitionSelectionRepository selectionRepository;
    private final TenantCertDefinitionRepository tenantDefinitionRepository;

    @Override
    public String domainKey() {
        return "certifications";
    }

    @Override
    @Transactional
    public void copyTemplates(Standard source, Standard target) {
        for (CertDefinitionSelection selection :
                selectionRepository.findByStandardId(source.getId())) {
            if (selectionRepository.existsByStandardIdAndDefinitionConfigKey(
                    target.getId(), selection.getDefinitionConfigKey())) {
                continue;
            }
            selectionRepository.save(
                    CertDefinitionSelection.builder()
                            .tenantId(target.getTenantId())
                            .standardId(target.getId())
                            .definitionConfigKey(selection.getDefinitionConfigKey())
                            .build());
        }
    }

    @Override
    @Transactional
    public void materializeForOrg(Long organizationId, Standard standard) {
        // Certifications are tenant-scoped; standard selections resolve at catalog read time.
    }

    @Override
    @Transactional
    public void reassignForOrg(Long organizationId, Standard standard) {
        // Certifications are tenant-scoped; standard selections resolve at catalog read time.
    }

    @Override
    @Transactional(readOnly = true)
    public List<StandardContentItem> describeContent(Standard standard) {
        List<StandardContentItem> items = new ArrayList<>();
        for (CertDefinitionSelection selection :
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
                                                            "description",
                                                            definition.getDescription() == null
                                                                    ? ""
                                                                    : definition.getDescription(),
                                                            "intervalDays",
                                                            definition.getIntervalDays() == null
                                                                    ? ""
                                                                    : String.valueOf(
                                                                            definition
                                                                                    .getIntervalDays()),
                                                            "reminderDaysBeforeExpiry",
                                                            String.valueOf(
                                                                    definition
                                                                            .getReminderDaysBeforeExpiry()),
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
                        .map(TenantCertDefinition::getConfigKey)
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
            TenantCertDefinition tenantDefinition =
                    tenantDefinitionRepository
                            .findByConfigKey(trimmed)
                            .orElseThrow(
                                    () ->
                                            new ConflictException(
                                                    "Unknown tenant certification definition: "
                                                            + trimmed));
            if (tenantDefinition.isArchived()) {
                throw new ConflictException(
                        "Tenant certification definition is archived: " + trimmed);
            }
            uniqueKeys.add(trimmed);
        }

        Set<String> existingKeys =
                selectionRepository.findByStandardId(standardId).stream()
                        .map(CertDefinitionSelection::getDefinitionConfigKey)
                        .collect(Collectors.toSet());

        for (String key : uniqueKeys) {
            if (!existingKeys.contains(key)) {
                selectionRepository.save(
                        CertDefinitionSelection.builder()
                                .tenantId(tenantId)
                                .standardId(standardId)
                                .definitionConfigKey(key)
                                .build());
            }
        }
        for (CertDefinitionSelection selection : selectionRepository.findByStandardId(standardId)) {
            if (!uniqueKeys.contains(selection.getDefinitionConfigKey())) {
                selectionRepository.delete(selection);
            }
        }
    }
}
