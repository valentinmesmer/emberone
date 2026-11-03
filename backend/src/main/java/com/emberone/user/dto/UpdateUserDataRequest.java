/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.user.dto;

import com.emberone.platform.validation.DateOfBirth;
import com.emberone.user.domain.Salutation;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import lombok.Data;

@Data
public class UpdateUserDataRequest {

    private Salutation salutation;

    @NotBlank(message = "First name is required") private String firstName;

    @NotBlank(message = "Last name is required") private String lastName;

    @DateOfBirth private LocalDate dateOfBirth;
}
