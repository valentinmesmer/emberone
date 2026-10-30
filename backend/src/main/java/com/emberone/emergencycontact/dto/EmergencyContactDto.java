/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.emergencycontact.dto;

import com.emberone.emergencycontact.domain.EmergencyContactRelationship;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContactDto {
    private Long id;
    private Long userId;
    private Integer priority;
    private EmergencyContactRelationship relationship;
    private String firstName;
    private String lastName;
    private String street;
    private String city;
    private String postalCode;
    private String country;
    private String phone;
    private String email;
}
