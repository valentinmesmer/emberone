/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.management.dto;

import com.emberone.management.domain.ManagementActivationEndedBy;
import com.emberone.management.domain.ManagementActivationStatus;
import com.emberone.permission.domain.PermissionType;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/** One row of the activation log. {@code userId} and the names are null for deleted users. */
@Value
@Builder
public class ManagementActivationLogItemDto {
    Long id;
    Long tenantId;
    String tenantName;
    Long userId;
    String userFirstName;
    String userLastName;
    Long managingOrganizationId;
    String managingOrganizationName;
    Long targetOrganizationId;
    String targetOrganizationName;
    Instant activatedAt;
    Instant validUntil;
    Instant endedAt;
    ManagementActivationEndedBy endedBy;
    ManagementActivationStatus status;
    List<PermissionType> permissionTypes;
    String reason;
}
