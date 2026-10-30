/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.standard.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One content item of a standard (organization group, permission group, mapping, or workflow
 * approval).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardContentItemDto {
    private String domainKey;
    private String configKey;
    private String title;
    private String subtitle;
    private List<String> permissionTypes;
    private Map<String, String> attributes;
    private boolean editable;
}
