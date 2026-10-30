/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** Shared pagination defaults for list endpoints. */
public final class ListEndpointLimits {

    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 200;

    private ListEndpointLimits() {}

    public static Pageable pageRequest(Integer page) {
        return pageRequest(page, null);
    }

    public static Pageable pageRequest(Integer page, Integer size) {
        int pageIndex = page != null && page >= 0 ? page : 0;
        int pageSize = size != null && size > 0 ? Math.min(size, MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        return PageRequest.of(pageIndex, pageSize);
    }
}
