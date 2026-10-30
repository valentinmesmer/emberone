/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.retention.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantRetentionProfileDto {
    /** Stable profile key (e.g. {@code BY}); later used when tenants can pick among profiles. */
    private String profileKey;

    private List<RetentionCategoryPeriodDto> periods;
}
