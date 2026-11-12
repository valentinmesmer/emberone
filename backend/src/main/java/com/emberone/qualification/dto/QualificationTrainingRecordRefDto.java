/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.qualification.dto;

import lombok.Builder;
import lombok.Value;

/** Newest training record that grants the course type with the given config key. */
@Value
@Builder
public class QualificationTrainingRecordRefDto {
    String configKey;
    Long recordId;
}
