/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.dutyfunction.service;

import com.emberone.dutyfunction.dto.*;
import com.emberone.dutyfunction.tenant.TenantFunction;
import com.emberone.dutyfunction.tenant.TenantFunctionRepository;
import com.emberone.federalstate.baseline.FunctionBaseline;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.security.TenantScopeGuard;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantDutyFunctionDefinitionService {

    private final TenantFunctionRepository functionRepository;

    @Transactional(readOnly = true)
    public List<TenantFunctionDto> list(long tenantId) {
        return withTenantContext(
                tenantId,
                () ->
                        functionRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                                .map(this::toDto)
                                .toList());
    }

    @Transactional
    public void seedBaselineIfMissing(long tenantId, List<FunctionBaseline> items) {
        withTenantContext(
                tenantId,
                () -> {
                    for (FunctionBaseline item : items) {
                        if (functionRepository.findByConfigKey(item.configKey()).isPresent()) {
                            continue;
                        }
                        functionRepository.save(
                                TenantFunction.builder()
                                        .tenantId(tenantId)
                                        .configKey(item.configKey())
                                        .name(item.name())
                                        .abbreviation(item.abbreviation())
                                        .sortOrder(item.sortOrder())
                                        .active(true)
                                        .build());
                    }
                    return null;
                });
    }

    @Transactional
    public TenantFunctionDto create(long tenantId, CreateTenantFunctionRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    String configKey = request.getConfigKey().trim();
                    if (functionRepository.existsByConfigKeyAndArchivedFalse(configKey)) {
                        throw new ConflictException("Function already exists: " + configKey);
                    }
                    TenantFunction saved =
                            functionRepository.save(
                                    TenantFunction.builder()
                                            .tenantId(tenantId)
                                            .configKey(configKey)
                                            .name(request.getName().trim())
                                            .abbreviation(request.getAbbreviation().trim())
                                            .sortOrder(request.getSortOrder())
                                            .active(request.isActive())
                                            .build());
                    return toDto(saved);
                });
    }

    @Transactional
    public TenantFunctionDto update(
            long tenantId, String configKey, UpdateTenantFunctionRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    TenantFunction function = requireFunction(configKey);
                    if (function.isArchived()) {
                        throw new ConflictException("Function is archived");
                    }
                    if (request.getName() != null) {
                        function.setName(request.getName().trim());
                    }
                    if (request.getAbbreviation() != null) {
                        function.setAbbreviation(request.getAbbreviation().trim());
                    }
                    if (request.getSortOrder() != null) {
                        function.setSortOrder(request.getSortOrder());
                    }
                    if (request.getActive() != null) {
                        function.setActive(request.getActive());
                    }
                    return toDto(functionRepository.save(function));
                });
    }

    @Transactional
    public void archive(long tenantId, String configKey) {
        withTenantContext(
                tenantId,
                () -> {
                    TenantFunction function = requireFunction(configKey);
                    LifecycleFields.archiveCatalogEntry(function, Instant.now());
                    functionRepository.save(function);
                    return null;
                });
    }

    @Transactional
    public void unarchive(long tenantId, String configKey) {
        withTenantContext(
                tenantId,
                () -> {
                    TenantFunction function = requireFunction(configKey);
                    LifecycleFields.unarchiveCatalogEntry(function, true);
                    functionRepository.save(function);
                    return null;
                });
    }

    private TenantFunction requireFunction(String configKey) {
        return functionRepository
                .findByConfigKey(configKey)
                .orElseThrow(() -> new ResourceNotFoundException("Function not found"));
    }

    private TenantFunctionDto toDto(TenantFunction function) {
        return TenantFunctionDto.builder()
                .configKey(function.getConfigKey())
                .name(function.getName())
                .abbreviation(function.getAbbreviation())
                .sortOrder(function.getSortOrder())
                .active(function.isActive())
                .archived(function.isArchived())
                .archivedAt(function.getArchivedAt())
                .build();
    }

    private <T> T withTenantContext(long tenantId, Supplier<T> action) {
        TenantScopeGuard.bind(tenantId);
        TenantScopeGuard.requireActiveTenantMatches(tenantId);
        return action.get();
    }
}
