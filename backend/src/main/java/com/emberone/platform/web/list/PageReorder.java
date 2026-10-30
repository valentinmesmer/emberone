/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

/**
 * Loads a page via specification, hydrates associations in a second query, and restores row order.
 */
public final class PageReorder {

    private PageReorder() {}

    public static <T, ID> List<T> reorderByIds(
            List<ID> ids, Collection<T> loaded, Function<T, ID> idExtractor) {
        Map<ID, T> byId = new HashMap<>();
        for (T item : loaded) {
            byId.put(idExtractor.apply(item), item);
        }
        List<T> ordered = new ArrayList<>(ids.size());
        for (ID id : ids) {
            T item = byId.get(id);
            if (item != null) {
                ordered.add(item);
            }
        }
        return ordered;
    }

    public static <T> Page<T> pageOf(List<T> content, Page<?> sourcePage) {
        return new PageImpl<>(content, sourcePage.getPageable(), sourcePage.getTotalElements());
    }
}
