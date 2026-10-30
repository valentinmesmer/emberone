/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.list;

import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.ListSort;
import com.emberone.platform.web.list.ListSpecs;
import com.emberone.qualification.dto.QualificationStatusItemDto;
import com.emberone.qualification.dto.SelfQualificationStatusItemDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class QualificationListSupport {

    private static final String DEFAULT_SORT = "name,asc";

    private static final Map<String, Comparator<QualificationStatusItemDto>> SORTS =
            Map.of(
                    "name",
                    Comparator.comparing(
                            QualificationStatusItemDto::getName, String.CASE_INSENSITIVE_ORDER),
                    "abbreviation",
                    Comparator.comparing(
                            QualificationStatusItemDto::getAbbreviation,
                            String.CASE_INSENSITIVE_ORDER),
                    "status",
                    Comparator.comparing(QualificationStatusItemDto::getStatus),
                    "id",
                    Comparator.comparing(
                            QualificationStatusItemDto::getTenantQualificationDefinitionId));

    private QualificationListSupport() {}

    public static List<QualificationStatusItemDto> filterAndSort(
            List<QualificationStatusItemDto> items,
            ListQuery query,
            QualificationListFilter filter) {
        List<QualificationStatusItemDto> result = new ArrayList<>(items);
        if (filter.status() != null) {
            result.removeIf(item -> item.getStatus() != filter.status());
        }
        List<String> tokens = ListSpecs.searchTokens(query.search());
        if (!tokens.isEmpty()) {
            result.removeIf(item -> !matchesSearch(item, tokens));
        }
        result.sort(resolveComparator(query.sort()));
        return result;
    }

    private static boolean matchesSearch(QualificationStatusItemDto item, List<String> tokens) {
        String haystack =
                (item.getName() + " " + item.getAbbreviation() + " " + item.getConfigKey())
                        .toLowerCase(Locale.ROOT);
        for (String token : tokens) {
            if (!haystack.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private static Comparator<QualificationStatusItemDto> resolveComparator(String clientSort) {
        ListSort sort = ListSort.parse(StringUtils.hasText(clientSort) ? clientSort : DEFAULT_SORT);
        Comparator<QualificationStatusItemDto> comparator = SORTS.get(sort.key());
        if (comparator == null) {
            throw new IllegalArgumentException("invalid sort field");
        }
        if (sort.direction().isDescending()) {
            comparator = comparator.reversed();
        }
        return comparator.thenComparing(
                QualificationStatusItemDto::getTenantQualificationDefinitionId);
    }

    public static List<SelfQualificationStatusItemDto> filterAndSortSelf(
            List<SelfQualificationStatusItemDto> rows,
            ListQuery query,
            QualificationListFilter filter) {
        List<SelfQualificationStatusItemDto> result = new ArrayList<>(rows);
        if (filter.status() != null) {
            result.removeIf(row -> row.getItem().getStatus() != filter.status());
        }
        List<String> tokens = ListSpecs.searchTokens(query.search());
        if (!tokens.isEmpty()) {
            result.removeIf(row -> !matchesSearch(row.getItem(), tokens));
        }
        Comparator<QualificationStatusItemDto> itemComparator = resolveComparator(query.sort());
        result.sort(
                Comparator.comparing(SelfQualificationStatusItemDto::getItem, itemComparator)
                        .thenComparing(SelfQualificationStatusItemDto::getOrganizationId));
        return result;
    }
}
