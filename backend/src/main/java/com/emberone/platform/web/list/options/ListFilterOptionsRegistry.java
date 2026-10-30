/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list.options;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ListFilterOptionsRegistry {

    private record ScopeKey(ListFilterOptionsScope scope, String key) {}

    private final Map<ScopeKey, ListFilterOptionsProvider> providersByScopeKey;

    public ListFilterOptionsRegistry(List<ListFilterOptionsProvider> providers) {
        Map<ScopeKey, ListFilterOptionsProvider> map = new HashMap<>();
        for (ListFilterOptionsProvider provider : providers) {
            for (ListFilterOptionsScope scope : provider.scopes()) {
                ScopeKey scopeKey = new ScopeKey(scope, provider.key());
                if (map.containsKey(scopeKey)) {
                    throw new IllegalStateException(
                            "Duplicate list filter options provider for "
                                    + scope
                                    + "."
                                    + provider.key());
                }
                map.put(scopeKey, provider);
            }
        }
        this.providersByScopeKey = Map.copyOf(map);
    }

    public Optional<ListFilterOptionsProvider> find(ListFilterOptionsScope scope, String key) {
        return Optional.ofNullable(providersByScopeKey.get(new ScopeKey(scope, key)));
    }
}
