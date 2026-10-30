/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Subset of Authentik {@code GET /api/v3/core/users/{pk}/} payload needed for group membership
 * sync.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthentikUserDetailResponse {

    /**
     * Group primary keys as returned by Authentik (strings, sometimes nested objects when
     * expanded).
     */
    private List<Object> groups;

    private static String groupElementToPk(Object element) {
        if (element == null) {
            return null;
        }
        if (element instanceof String s) {
            return s;
        }
        if (element instanceof Number n) {
            return n.toString();
        }
        if (element instanceof Map<?, ?> m) {
            Object pk = m.get("pk");
            return pk == null ? null : pk.toString();
        }
        return element.toString();
    }

    public List<String> groupPks() {
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>(groups.size());
        for (Object element : groups) {
            String pk = groupElementToPk(element);
            if (pk != null && !pk.isBlank()) {
                out.add(pk);
            }
        }
        return out;
    }
}
