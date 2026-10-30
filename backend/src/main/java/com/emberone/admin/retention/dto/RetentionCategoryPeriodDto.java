/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.retention.dto;

import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.profile.RetentionPeriodUnit;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetentionCategoryPeriodDto {
    private RetentionCategory category;
    private int amount;
    private RetentionPeriodUnit unit;
    private List<String> tables;
}
