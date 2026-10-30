/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.dto;

import com.emberone.permission.domain.PermissionType;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ManagementActivationDto {
    Long id;
    Long managingOrganizationId;
    String managingOrganizationName;
    Long targetOrganizationId;
    String targetOrganizationName;
    Instant activatedAt;
    Instant validUntil;
    List<PermissionType> permissionTypes;
    String reason;
}
