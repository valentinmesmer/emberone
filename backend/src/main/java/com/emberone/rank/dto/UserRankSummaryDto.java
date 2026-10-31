/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.rank.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserRankSummaryDto {
    Long tenantRankId;
    String configKey;
    String name;
    String abbreviation;
    int sortOrder;
    LocalDate startDate;
}
