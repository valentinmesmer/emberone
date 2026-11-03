/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.organization.dto;

import lombok.Data;

@Data
public class MoveOrganizationRequest {

    /** {@code null} moves the organization to tenant root. */
    private Long newParentOrganizationId;
}
