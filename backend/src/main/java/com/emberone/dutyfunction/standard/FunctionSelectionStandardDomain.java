/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.dutyfunction.standard;

import com.emberone.dutyfunction.tenant.TenantFunction;
import com.emberone.dutyfunction.tenant.TenantFunctionRepository;
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
@Order(56)
@RequiredArgsConstructor
public class FunctionSelectionStandardDomain implements StandardDomain {

    private final FunctionSelectionRepository selectionRepository;
    private final TenantFunctionRepository functionRepository;
    private final FunctionGrantRepository grantRepository;

    @Override
    public String domainKey() {
        return "functions";
    }

    @Override
    @Transactional
    public void copyTemplates(Standard source, Standard target) {
        for (FunctionSelection selection : selectionRepository.findByStandardId(source.getId())) {
            if (selectionRepository.existsByStandardIdAndFunctionConfigKey(
                    target.getId(), selection.getFunctionConfigKey())) {
                continue;
            }
            selectionRepository.save(
                    FunctionSelection.builder()
                            .tenantId(target.getTenantId())
                            .standardId(target.getId())
                            .functionConfigKey(selection.getFunctionConfigKey())
                            .build());
        }
    }

    @Override
    @Transactional
    public void materializeForOrg(Long organizationId, Standard standard) {}

    @Override
    @Transactional
    public void reassignForOrg(Long organizationId, Standard standard) {}

    @Override
    @Transactional(readOnly = true)
    public List<StandardContentItem> describeContent(Standard standard) {
        List<StandardContentItem> items = new ArrayList<>();
        for (FunctionSelection selection : selectionRepository.findByStandardId(standard.getId())) {
            functionRepository
                    .findByConfigKey(selection.getFunctionConfigKey())
                    .filter(function -> !function.isArchived())
                    .ifPresent(
                            function ->
                                    items.add(
                                            new StandardContentItem(
                                                    domainKey(),
                                                    function.getConfigKey(),
                                                    function.getName(),
                                                    function.getAbbreviation(),
                                                    List.of(),
                                                    Map.of(
                                                            "active",
                                                            String.valueOf(function.isActive())),
                                                    false)));
        }
        items.sort(Comparator.comparing(StandardContentItem::configKey));
        return items;
    }

    @Transactional
    public void selectAllActiveFunctions(Standard standard) {
        List<String> keys =
                functionRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                        .filter(function -> !function.isArchived())
                        .map(TenantFunction::getConfigKey)
                        .toList();
        replaceSelections(standard, keys);
    }

    @Transactional
    public void replaceSelections(Standard standard, List<String> functionConfigKeys) {
        Long tenantId = standard.getTenantId();
        Long standardId = standard.getId();
        Set<String> uniqueKeys = new LinkedHashSet<>();
        for (String configKey : functionConfigKeys) {
            if (configKey == null || configKey.isBlank()) {
                continue;
            }
            String trimmed = configKey.trim();
            TenantFunction function =
                    functionRepository
                            .findByConfigKey(trimmed)
                            .orElseThrow(
                                    () ->
                                            new ConflictException(
                                                    "Unknown tenant function: " + trimmed));
            if (function.isArchived()) {
                throw new ConflictException("Tenant function is archived: " + trimmed);
            }
            uniqueKeys.add(trimmed);
        }

        Set<String> existingKeys =
                selectionRepository.findByStandardId(standardId).stream()
                        .map(FunctionSelection::getFunctionConfigKey)
                        .collect(Collectors.toSet());

        for (String key : uniqueKeys) {
            if (!existingKeys.contains(key)) {
                selectionRepository.save(
                        FunctionSelection.builder()
                                .tenantId(tenantId)
                                .standardId(standardId)
                                .functionConfigKey(key)
                                .build());
            }
        }
        for (FunctionSelection selection : selectionRepository.findByStandardId(standardId)) {
            if (!uniqueKeys.contains(selection.getFunctionConfigKey())) {
                grantRepository.findByStandardId(standardId).stream()
                        .filter(
                                grant ->
                                        grant.getFunctionConfigKey()
                                                .equals(selection.getFunctionConfigKey()))
                        .forEach(grantRepository::delete);
                selectionRepository.delete(selection);
            }
        }
    }
}
