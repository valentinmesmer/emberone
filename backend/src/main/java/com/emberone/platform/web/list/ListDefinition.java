/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list;

import com.emberone.platform.web.ListEndpointLimits;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Declarative list configuration: search fields, sort whitelist, and helpers for {@link Pageable}
 * and search {@link Specification}.
 */
public final class ListDefinition<E> {

    private final Class<E> entityType;
    private final List<String> searchAttributePaths;
    private final Map<String, SortMapping> sortWhitelist;
    private final String defaultSort;

    private ListDefinition(
            Class<E> entityType,
            List<String> searchAttributePaths,
            Map<String, SortMapping> sortWhitelist,
            String defaultSort) {
        this.entityType = entityType;
        this.searchAttributePaths = List.copyOf(searchAttributePaths);
        this.sortWhitelist = Map.copyOf(sortWhitelist);
        this.defaultSort = defaultSort;
    }

    public static <E> Builder<E> forEntity(Class<E> entityType) {
        return new Builder<>(entityType);
    }

    public Class<E> entityType() {
        return entityType;
    }

    public Pageable pageable(ListQuery query) {
        return pageable(query, null);
    }

    /** Sort precedence: client {@code sort} &gt; {@code sortOverride} &gt; definition default. */
    public Pageable pageable(ListQuery query, String sortOverride) {
        int pageIndex = query.page() != null && query.page() >= 0 ? query.page() : 0;
        int pageSize =
                query.size() != null && query.size() > 0
                        ? Math.min(query.size(), ListEndpointLimits.MAX_PAGE_SIZE)
                        : ListEndpointLimits.DEFAULT_PAGE_SIZE;
        ListSort listSort = resolve(query.sort(), sortOverride);
        SortMapping mapping = sortWhitelist.get(listSort.key());
        List<Sort.Order> orders = new ArrayList<>();
        for (String property : mapping.jpaProperties()) {
            Sort.Order order = new Sort.Order(listSort.direction(), property);
            orders.add(mapping.ignoreCase() ? order.ignoreCase() : order);
        }
        if (!mapping.jpaProperties().contains("id")) {
            orders.add(new Sort.Order(Sort.Direction.ASC, "id"));
        }
        return PageRequest.of(pageIndex, pageSize, Sort.by(orders));
    }

    public Specification<E> searchSpecification(ListQuery query) {
        return ListSpecs.searchTokens(query.search(), searchAttributePaths);
    }

    private ListSort resolve(String clientSort, String sortOverride) {
        String effective =
                StringUtils.hasText(clientSort)
                        ? clientSort
                        : StringUtils.hasText(sortOverride) ? sortOverride : defaultSort;
        ListSort listSort = ListSort.parse(effective);
        if (!sortWhitelist.containsKey(listSort.key())) {
            throw new IllegalArgumentException("invalid sort field");
        }
        return listSort;
    }

    public static final class Builder<E> {

        private final Class<E> entityType;
        private final List<String> searchAttributePaths = new ArrayList<>();
        private final Map<String, SortMapping> sortWhitelist = new LinkedHashMap<>();
        private String defaultSort;

        private Builder(Class<E> entityType) {
            this.entityType = entityType;
        }

        public Builder<E> searchField(String jpaAttributePath) {
            searchAttributePaths.add(jpaAttributePath);
            return this;
        }

        /** Sort key over one or more JPA properties (same direction for all, in order). */
        public Builder<E> sortKey(String clientKey, String... jpaProperties) {
            return putKey(clientKey, new SortMapping(List.of(jpaProperties), false));
        }

        /** Like {@link #sortKey} but case-insensitive (names, titles, places). */
        public Builder<E> textSortKey(String clientKey, String... jpaProperties) {
            return putKey(clientKey, new SortMapping(List.of(jpaProperties), true));
        }

        private Builder<E> putKey(String clientKey, SortMapping mapping) {
            sortWhitelist.put(clientKey, mapping);
            return this;
        }

        public Builder<E> defaultSort(String clientSort) {
            this.defaultSort = clientSort;
            return this;
        }

        public ListDefinition<E> build() {
            Objects.requireNonNull(defaultSort, "defaultSort");
            if (sortWhitelist.isEmpty()) {
                throw new IllegalArgumentException("sort whitelist required");
            }
            if (!sortWhitelist.containsKey(ListSort.parse(defaultSort).key())) {
                throw new IllegalArgumentException("default sort not in whitelist");
            }
            return new ListDefinition<>(
                    entityType, searchAttributePaths, sortWhitelist, defaultSort);
        }
    }

    private record SortMapping(List<String> jpaProperties, boolean ignoreCase) {}
}
