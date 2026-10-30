/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserQualificationStatusDto {
    Long userId;
    Long organizationId;
    List<QualificationStatusItemDto> items;
}
