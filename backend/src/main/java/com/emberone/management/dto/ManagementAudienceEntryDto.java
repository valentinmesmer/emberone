/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.dto;

import com.emberone.management.domain.ManagementAudienceKind;
import com.emberone.permission.domain.PermissionType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagementAudienceEntryDto {
    private Long id;
    private ManagementAudienceKind kind;
    private Long refId;
    private List<PermissionType> permissionTypes;
    private Integer maxDurationMinutes;
}
