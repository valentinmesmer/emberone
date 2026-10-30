/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.organization.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Full tenant org tree in one response (roots only; children nested). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrganizationTreeResponseDto {
    private long tenantId;
    private String tenantName;
    private List<AdminOrganizationTreeNodeDto> roots;
}
