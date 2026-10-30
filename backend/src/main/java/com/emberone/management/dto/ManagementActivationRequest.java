/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.dto;

import com.emberone.permission.domain.PermissionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class ManagementActivationRequest {

    @NotNull(message = "Managing organization is required") private Long managingOrganizationId;

    @NotNull(message = "Target organization is required") private Long targetOrganizationId;

    @NotNull(message = "Duration is required") @Min(value = 1, message = "Duration must be at least 1 minute") private Integer durationMinutes;

    /**
     * Chosen permissions, a non-empty subset of the target's permissions. Nothing is preselected.
     */
    @NotEmpty(message = "At least one permission is required") private List<PermissionType> permissionTypes;

    /** Free text, 10 to 500 characters after trimming; checked by the service. */
    private String reason;
}
