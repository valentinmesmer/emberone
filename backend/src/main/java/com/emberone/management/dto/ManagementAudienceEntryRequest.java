/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.dto;

import com.emberone.management.domain.ManagementAudienceKind;
import com.emberone.permission.domain.PermissionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class ManagementAudienceEntryRequest {

    @NotNull(message = "Kind is required") private ManagementAudienceKind kind;

    /** Organization id (ORG, ORG_SUBTREE) or organization type id (ORG_TYPE); null for TENANT. */
    private Long refId;

    /** Allowed permission types; the activation picks from them. */
    @NotEmpty(message = "At least one permission type is required") private List<PermissionType> permissionTypes;

    @NotNull(message = "Maximum duration is required") @Min(value = 1, message = "Maximum duration must be at least 1 minute") private Integer maxDurationMinutes;
}
