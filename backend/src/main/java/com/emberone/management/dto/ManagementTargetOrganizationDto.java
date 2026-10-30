/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.dto;

import com.emberone.permission.domain.PermissionType;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * An organization the caller may open through one managing organization. {@code maxDurationMinutes}
 * and {@code permissionTypes} are the largest values across the audience entries that cover it; the
 * activation itself must still fit one single entry.
 */
@Value
@Builder
public class ManagementTargetOrganizationDto {
    Long organizationId;
    String organizationName;
    Long managingOrganizationId;
    String managingOrganizationName;
    Integer maxDurationMinutes;
    List<PermissionType> permissionTypes;
}
