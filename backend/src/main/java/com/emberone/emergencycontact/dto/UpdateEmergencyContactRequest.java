/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.emergencycontact.dto;

import com.emberone.emergencycontact.domain.EmergencyContactRelationship;
import com.emberone.platform.validation.EmailFormat;
import com.emberone.platform.validation.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmergencyContactRequest {

    @NotNull(message = "Relationship is required") private EmergencyContactRelationship relationship;

    @NotBlank(message = "First name is required") private String firstName;

    @NotBlank(message = "Last name is required") private String lastName;

    @NotBlank(message = "Street is required") private String street;

    @NotBlank(message = "City is required") private String city;

    @NotBlank(message = "Postal code is required") private String postalCode;

    @NotBlank(message = "Country is required") private String country;

    @NotBlank(message = "Phone is required") @PhoneNumber(message = "Invalid phone number format")
    private String phone;

    @NotBlank(message = "Email is required") @EmailFormat(message = "Email must be a valid email address")
    private String email;
}
