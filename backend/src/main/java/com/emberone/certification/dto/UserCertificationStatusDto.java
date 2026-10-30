/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * A user's full certification status: the flat list of required certifications plus rollup counts
 * (the latter drive the My-Area hub badge).
 */
@Data
@Builder
public class UserCertificationStatusDto {
    private Long userId;
    private List<CertificationStatusItemDto> items;
    private int missingCount;
    private int expiredCount;
    private int expiringSoonCount;
    private int validCount;
}
