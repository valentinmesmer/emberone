/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.dto;

import com.emberone.absence.domain.AbsenceReason;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAbsenceDto {
    private Long id;
    private Long userId;
    private List<Long> organizationIds;
    private LocalDate startDate;
    private LocalDate endDate;
    private AbsenceReason reason;
    private String comment;
}
