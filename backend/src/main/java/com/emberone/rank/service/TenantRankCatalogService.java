/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.service;

import com.emberone.federalstate.baseline.RankBaseline;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.rank.dto.*;
import com.emberone.rank.tenant.TenantRank;
import com.emberone.rank.tenant.TenantRankRepository;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantRankCatalogService {

    private final TenantRankRepository rankRepository;

    @Transactional(readOnly = true)
    public List<TenantRankDto> listCatalog(long tenantId) {
        return withTenantContext(
                tenantId,
                () ->
                        rankRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                                .map(TenantRankCatalogService::toRankDto)
                                .toList());
    }

    @Transactional
    public void seedBaselineIfMissing(long tenantId, List<RankBaseline> ranks) {
        withTenantContext(
                tenantId,
                () -> {
                    for (RankBaseline item : ranks) {
                        if (rankRepository.findByConfigKey(item.configKey()).isPresent()) {
                            continue;
                        }
                        rankRepository.save(
                                TenantRank.builder()
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
    public TenantRankDto createRank(long tenantId, CreateTenantRankRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    String configKey = request.getConfigKey().trim();
                    if (rankRepository.existsByConfigKeyAndArchivedFalse(configKey)) {
                        throw new ConflictException("Rank already exists: " + configKey);
                    }
                    TenantRank saved =
                            rankRepository.save(
                                    TenantRank.builder()
                                            .tenantId(tenantId)
                                            .configKey(configKey)
                                            .name(request.getName().trim())
                                            .abbreviation(request.getAbbreviation().trim())
                                            .sortOrder(request.getSortOrder())
                                            .active(request.isActive())
                                            .build());
                    return toRankDto(saved);
                });
    }

    @Transactional
    public TenantRankDto updateRank(
            long tenantId, String configKey, UpdateTenantRankRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    TenantRank rank = requireRank(configKey);
                    if (rank.isArchived()) {
                        throw new ConflictException("Rank is archived");
                    }
                    if (request.getName() != null) {
                        rank.setName(request.getName().trim());
                    }
                    if (request.getAbbreviation() != null) {
                        rank.setAbbreviation(request.getAbbreviation().trim());
                    }
                    if (request.getSortOrder() != null) {
                        rank.setSortOrder(request.getSortOrder());
                    }
                    if (request.getActive() != null) {
                        rank.setActive(request.getActive());
                    }
                    return toRankDto(rankRepository.save(rank));
                });
    }

    @Transactional
    public void archiveRank(long tenantId, String configKey) {
        withTenantContext(
                tenantId,
                () -> {
                    TenantRank rank = requireRank(configKey);
                    LifecycleFields.archiveCatalogEntry(rank, Instant.now());
                    rankRepository.save(rank);
                    return null;
                });
    }

    @Transactional
    public void unarchiveRank(long tenantId, String configKey) {
        withTenantContext(
                tenantId,
                () -> {
                    TenantRank rank = requireRank(configKey);
                    LifecycleFields.unarchiveCatalogEntry(rank, true);
                    rankRepository.save(rank);
                    return null;
                });
    }

    @Transactional(readOnly = true)
    public void requireActiveRank(long tenantRankId) {
        TenantRank rank =
                rankRepository
                        .findById(tenantRankId)
                        .orElseThrow(() -> new ConflictException("Unknown rank"));
        if (!rank.isActive() || rank.isArchived()) {
            throw new ConflictException("Rank is not active");
        }
    }

    @Transactional(readOnly = true)
    public List<TenantRankDto> listActiveRanks(long tenantId) {
        return withTenantContext(
                tenantId,
                () ->
                        rankRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                                .filter(r -> r.isActive() && !r.isArchived())
                                .map(TenantRankCatalogService::toRankDto)
                                .toList());
    }

    private TenantRank requireRank(String configKey) {
        return rankRepository
                .findByConfigKey(configKey)
                .orElseThrow(() -> new ResourceNotFoundException("Rank not found"));
    }

    private static TenantRankDto toRankDto(TenantRank rank) {
        return TenantRankDto.builder()
                .id(rank.getId())
                .configKey(rank.getConfigKey())
                .name(rank.getName())
                .abbreviation(rank.getAbbreviation())
                .sortOrder(rank.getSortOrder())
                .active(rank.isActive())
                .archived(rank.isArchived())
                .archivedAt(rank.getArchivedAt())
                .build();
    }

    private <T> T withTenantContext(long tenantId, Supplier<T> action) {
        TenantScopeGuard.bind(tenantId);
        TenantScopeGuard.requireActiveTenantMatches(tenantId);
        return action.get();
    }
}
