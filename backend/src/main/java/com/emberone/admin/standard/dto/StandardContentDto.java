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

/** A standard plus its full content (template items), for the admin view/edit standard page. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardContentDto {
    private StandardDto standard;
    private List<StandardContentItemDto> items;
}
