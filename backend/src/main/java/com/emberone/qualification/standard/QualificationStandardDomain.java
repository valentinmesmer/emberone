/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.standard;

import com.emberone.platform.exception.ConflictException;
import com.emberone.qualification.event.QualificationCatalogChangedEvent;
import com.emberone.qualification.tenant.TenantQualificationDefinition;
import com.emberone.qualification.tenant.TenantQualificationDefinitionRepository;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.service.StandardOrganizationResolver;
import com.emberone.standard.spi.StandardContentItem;
import com.emberone.standard.spi.StandardDomain;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(52)
@RequiredArgsConstructor
public class QualificationStandardDomain implements StandardDomain {

    private final QualificationSelectionRepository selectionRepository;
    private final TenantQualificationDefinitionRepository definitionRepository;
    private final StandardOrganizationResolver organizationResolver;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public String domainKey() {
        return "qualifications";
    }

    @Override
    @Transactional
    public void copyTemplates(Standard source, Standard target) {
        for (QualificationSelection selection :
                selectionRepository.findByStandardId(source.getId())) {
            if (selectionRepository.existsByStandardIdAndDefinitionConfigKey(
                    target.getId(), selection.getDefinitionConfigKey())) {
                continue;
            }
            selectionRepository.save(
                    QualificationSelection.builder()
                            .tenantId(target.getTenantId())
                            .standardId(target.getId())
                            .definitionConfigKey(selection.getDefinitionConfigKey())
                            .build());
        }
    }

    @Override
    @Transactional
    public void materializeForOrg(Long organizationId, Standard standard) {
        // Qualifications resolve at catalog read time.
    }

    @Override
    @Transactional
    public void reassignForOrg(Long organizationId, Standard standard) {
        // Qualifications resolve at catalog read time.
    }

    @Override
    @Transactional(readOnly = true)
    public List<StandardContentItem> describeContent(Standard standard) {
        List<StandardContentItem> items = new ArrayList<>();
        for (QualificationSelection selection :
                selectionRepository.findByStandardId(standard.getId())) {
            definitionRepository
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
                                                            "requiresOrgDecision",
                                                                    String.valueOf(
                                                                            definition
                                                                                    .isRequiresOrgDecision()),
                                                            "active",
                                                                    String.valueOf(
                                                                            definition.isActive())),
                                                    false)));
        }
        items.sort(Comparator.comparing(StandardContentItem::configKey));
        return items;
    }

    @Transactional
    public void selectAllActiveDefinitions(Standard standard) {
        List<String> keys =
                definitionRepository.findAllByOrderByNameAsc().stream()
                        .filter(definition -> !definition.isArchived())
                        .map(TenantQualificationDefinition::getConfigKey)
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
            TenantQualificationDefinition definition =
                    definitionRepository
                            .findByConfigKey(trimmed)
                            .orElseThrow(
                                    () ->
                                            new ConflictException(
                                                    "Unknown tenant qualification definition: "
                                                            + trimmed));
            if (definition.isArchived()) {
                throw new ConflictException(
                        "Tenant qualification definition is archived: " + trimmed);
            }
            uniqueKeys.add(trimmed);
        }

        Set<String> existingKeys =
                selectionRepository.findByStandardId(standardId).stream()
                        .map(QualificationSelection::getDefinitionConfigKey)
                        .collect(Collectors.toSet());

        for (String key : uniqueKeys) {
            if (!existingKeys.contains(key)) {
                selectionRepository.save(
                        QualificationSelection.builder()
                                .tenantId(tenantId)
                                .standardId(standardId)
                                .definitionConfigKey(key)
                                .build());
            }
        }
        for (QualificationSelection selection : selectionRepository.findByStandardId(standardId)) {
            if (!uniqueKeys.contains(selection.getDefinitionConfigKey())) {
                selectionRepository.delete(selection);
            }
        }

        List<Long> organizationIds =
                organizationResolver.resolveOrganizationsForStandard(standard).stream()
                        .map(org -> org.getId())
                        .toList();
        eventPublisher.publishEvent(
                new QualificationCatalogChangedEvent(tenantId, organizationIds));
    }
}
