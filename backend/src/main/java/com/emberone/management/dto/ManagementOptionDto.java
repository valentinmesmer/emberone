/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** An id and display name for the pickers of the management audience editor. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagementOptionDto {
    private Long id;
    private String name;
}
