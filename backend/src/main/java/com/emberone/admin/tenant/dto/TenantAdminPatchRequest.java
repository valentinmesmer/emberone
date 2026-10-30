/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TenantAdminPatchRequest {

    @Size(max = 255, message = "Name must not exceed 255 characters") private String name;
}
