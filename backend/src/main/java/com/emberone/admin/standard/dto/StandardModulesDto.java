/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.standard.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Modules enabled for a standard (excludes {@code CORE}, which is always implicitly enabled). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardModulesDto {
    private List<String> modules;
}
