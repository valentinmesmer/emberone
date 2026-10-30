/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list.options;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ListFilterOptionsService {

    private final ListFilterOptionsRegistry registry;

    @Transactional(readOnly = true)
    public ListFilterOptionsDto resolve(ListFilterOptionsContext context, List<String> include) {
        if (include == null || include.isEmpty()) {
            return new ListFilterOptionsDto(Map.of());
        }
        Set<String> wanted = new HashSet<>();
        for (String raw : include) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            wanted.add(raw.trim());
        }
        Map<String, List<ListFilterOptionDto>> options = new HashMap<>();
        for (String key : wanted) {
            ListFilterOptionsProvider provider =
                    registry.find(context.scope(), key)
                            .orElseThrow(
                                    () ->
                                            new ResponseStatusException(
                                                    HttpStatus.BAD_REQUEST,
                                                    "invalid include filter key: " + key));
            options.put(key, provider.resolve(context));
        }
        return new ListFilterOptionsDto(options);
    }
}
