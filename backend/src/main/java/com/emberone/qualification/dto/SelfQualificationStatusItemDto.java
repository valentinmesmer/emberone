/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.qualification.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SelfQualificationStatusItemDto {
    Long organizationId;
    QualificationStatusItemDto item;
}
