/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.dutyfunction.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TenantFunctionDto {
    String configKey;
    String name;
    String abbreviation;
    int sortOrder;
    boolean active;
    boolean archived;
    Instant archivedAt;
}
