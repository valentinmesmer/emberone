/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.standard.dto;

import lombok.Data;

/**
 * Sets a per-org standard override. A {@code null} {@code standardId} clears the override so the
 * organization inherits the standard assigned to its org type.
 */
@Data
public class SetOrganizationStandardRequest {

    /**
     * Custom standard to assign as the override; {@code null} clears the override (inherit type
     * default).
     */
    private Long standardId;
}
