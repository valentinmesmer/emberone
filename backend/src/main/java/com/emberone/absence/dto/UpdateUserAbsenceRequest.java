/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.dto;

import com.emberone.absence.domain.AbsenceReason;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class UpdateUserAbsenceRequest {

    @NotEmpty(message = "At least one organization is required") private List<@NotNull(message = "Organization id must not be null") Long> organizationIds;

    @NotNull(message = "Start date is required") private LocalDate startDate;

    @NotNull(message = "End date is required") private LocalDate endDate;

    @NotNull(message = "Reason is required") private AbsenceReason reason;

    @Size(max = 2000, message = "Comment must be at most 2000 characters") private String comment;
}
