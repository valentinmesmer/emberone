/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
