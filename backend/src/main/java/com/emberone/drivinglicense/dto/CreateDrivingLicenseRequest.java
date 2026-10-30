/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.drivinglicense.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class CreateDrivingLicenseRequest {

    @NotNull(message = "issueDate is required") private LocalDate issueDate;

    private LocalDate documentExpirationDate;

    @NotBlank(message = "licenseNumber is required") private String licenseNumber;

    @NotBlank(message = "issuingAuthority is required") private String issuingAuthority;

    @NotEmpty(message = "At least one class is required") @Valid private List<DrivingLicenseClassInput> classes;

    private List<Long> userDocumentIds;

    private String notes;
}
