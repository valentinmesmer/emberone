/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list;

import com.emberone.platform.web.ListEndpointLimits;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/** Slices an in-memory list using the same page/size rules as {@link ListQuery}. */
public final class ListPages {

    private ListPages() {}

    public static <T> Page<T> slice(List<T> items, ListQuery query) {
        int pageIndex = query.page() != null && query.page() >= 0 ? query.page() : 0;
        int pageSize =
                query.size() != null && query.size() > 0
                        ? Math.min(query.size(), ListEndpointLimits.MAX_PAGE_SIZE)
                        : ListEndpointLimits.DEFAULT_PAGE_SIZE;
        int total = items.size();
        int from = Math.min(pageIndex * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<T> content = items.subList(from, to);
        return new PageImpl<>(content, PageRequest.of(pageIndex, pageSize), total);
    }
}
