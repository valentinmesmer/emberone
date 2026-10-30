/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.dto;

import com.emberone.platform.validation.DateOfBirth;
import com.emberone.platform.validation.EmailFormat;
import com.emberone.user.domain.Salutation;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import lombok.Data;

@Data
public class CreateUserRequest {

    private Salutation salutation;

    @NotBlank(message = "First name is required") private String firstName;

    @NotBlank(message = "Last name is required") private String lastName;

    @DateOfBirth private LocalDate dateOfBirth;

    @NotBlank(message = "Private email is required") @EmailFormat(message = "Private email must be a valid email address")
    private String privateEmail;

    @NotBlank(message = "Street is required") private String street;

    @NotBlank(message = "City is required") private String city;

    @NotBlank(message = "Postal code is required") private String postalCode;

    @NotBlank(message = "Country is required") private String country;
}
