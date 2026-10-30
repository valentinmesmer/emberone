/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.dto;

import lombok.Data;

@Data
public class MoveOrganizationRequest {

    /** {@code null} moves the organization to tenant root. */
    private Long newParentOrganizationId;
}
