/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CertificationOverviewCountsDto {
    long all;
    long missing;
    long expired;
    long expiringSoon;
    long valid;
}
