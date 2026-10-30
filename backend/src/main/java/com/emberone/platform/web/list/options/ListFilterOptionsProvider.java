/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list.options;

import java.util.List;
import java.util.Set;

/**
 * Supplies options for one list filter param. Register as a Spring bean; {@link
 * ListFilterOptionsRegistry} collects providers by {@link #key()} and {@link #scopes()}. Only
 * return values the caller is allowed to use in the matching list GET - see {@link
 * ListFilterOptionsAuthorization}.
 */
public interface ListFilterOptionsProvider {

    /** Query param / {@code ListDefinition} filter key. */
    String key();

    /** Scopes this provider applies to (same key may differ per scope). */
    Set<ListFilterOptionsScope> scopes();

    List<ListFilterOptionDto> resolve(ListFilterOptionsContext context);
}
