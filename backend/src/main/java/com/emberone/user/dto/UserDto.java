/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.dto;

import com.emberone.user.domain.Salutation;
import com.emberone.userorganizationgroupmembership.dto.UserOrganizationGroupMembershipDto;
import com.emberone.userorganizationmembership.dto.UserOrganizationMembershipDto;
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
public class UserDto {
    private Long id;
    private Long personalNumber;
    private Salutation salutation;
    private String firstName;
    private String lastName;
    private String username;
    private LocalDate dateOfBirth;
    private UserContactInformationDto contactInformation;
    private List<UserOrganizationMembershipDto> organizationMemberships;
    private List<UserOrganizationGroupMembershipDto> organizationGroupMemberships;
}
