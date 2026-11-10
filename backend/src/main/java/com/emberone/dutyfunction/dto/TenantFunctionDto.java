/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
    boolean active;
    boolean archived;
    Instant archivedAt;
}
