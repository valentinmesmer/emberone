/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
