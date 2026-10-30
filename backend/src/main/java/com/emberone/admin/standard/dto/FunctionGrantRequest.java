/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.standard.dto;

import com.emberone.organizationgroup.domain.GroupMembershipType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FunctionGrantRequest {

    @NotBlank private String organizationGroupConfigKey;

    @NotNull private GroupMembershipType membershipType;

    @NotBlank private String functionConfigKey;
}
