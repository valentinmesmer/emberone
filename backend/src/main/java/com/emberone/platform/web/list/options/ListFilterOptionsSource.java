/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.web.list.options;

/**
 * Filter options for exactly one paginated list. Exposed via {@code GET …/list-filter-options} on
 * the same controller as the list GET; use the same {@code @PreAuthorize} on both methods.
 */
public interface ListFilterOptionsSource {

    ListFilterOptionsDto filterOptions();
}
