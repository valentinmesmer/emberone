/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.standard;

import com.emberone.platform.exception.ConflictException;
import com.emberone.rank.tenant.TenantRank;
import com.emberone.rank.tenant.TenantRankRepository;
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
@Order(54)
@RequiredArgsConstructor
public class RankStandardDomain implements StandardDomain {

    private final RankSelectionRepository selectionRepository;
    private final TenantRankRepository rankRepository;

    @Override
    public String domainKey() {
        return "ranks";
    }

    @Override
    @Transactional
    public void copyTemplates(Standard source, Standard target) {
        for (RankSelection selection : selectionRepository.findByStandardId(source.getId())) {
            if (selectionRepository.existsByStandardIdAndRankConfigKey(
                    target.getId(), selection.getRankConfigKey())) {
                continue;
            }
            selectionRepository.save(
                    RankSelection.builder()
                            .tenantId(target.getTenantId())
                            .standardId(target.getId())
                            .rankConfigKey(selection.getRankConfigKey())
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
        for (RankSelection selection : selectionRepository.findByStandardId(standard.getId())) {
            rankRepository
                    .findByConfigKey(selection.getRankConfigKey())
                    .filter(rank -> !rank.isArchived())
                    .ifPresent(
                            rank ->
                                    items.add(
                                            new StandardContentItem(
                                                    domainKey(),
                                                    rank.getConfigKey(),
                                                    rank.getName(),
                                                    rank.getAbbreviation(),
                                                    List.of(),
                                                    Map.of(
                                                            "active",
                                                            String.valueOf(rank.isActive())),
                                                    false)));
        }
        items.sort(Comparator.comparing(StandardContentItem::configKey));
        return items;
    }

    @Transactional
    public void selectAllActiveRanks(Standard standard) {
        List<String> keys =
                rankRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                        .filter(rank -> !rank.isArchived())
                        .map(TenantRank::getConfigKey)
                        .toList();
        replaceSelections(standard, keys);
    }

    @Transactional
    public void replaceSelections(Standard standard, List<String> rankConfigKeys) {
        Long tenantId = standard.getTenantId();
        Long standardId = standard.getId();
        Set<String> uniqueKeys = new LinkedHashSet<>();
        for (String configKey : rankConfigKeys) {
            if (configKey == null || configKey.isBlank()) {
                continue;
            }
            String trimmed = configKey.trim();
            TenantRank rank =
                    rankRepository
                            .findByConfigKey(trimmed)
                            .orElseThrow(
                                    () -> new ConflictException("Unknown tenant rank: " + trimmed));
            if (rank.isArchived()) {
                throw new ConflictException("Tenant rank is archived: " + trimmed);
            }
            uniqueKeys.add(trimmed);
        }

        Set<String> existingKeys =
                selectionRepository.findByStandardId(standardId).stream()
                        .map(RankSelection::getRankConfigKey)
                        .collect(Collectors.toSet());

        for (String key : uniqueKeys) {
            if (!existingKeys.contains(key)) {
                selectionRepository.save(
                        RankSelection.builder()
                                .tenantId(tenantId)
                                .standardId(standardId)
                                .rankConfigKey(key)
                                .build());
            }
        }
        for (RankSelection selection : selectionRepository.findByStandardId(standardId)) {
            if (!uniqueKeys.contains(selection.getRankConfigKey())) {
                selectionRepository.delete(selection);
            }
        }
    }
}
