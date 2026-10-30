/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.dto;

import com.emberone.organizationgroup.dto.GroupTreeDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationTreeDto {
    private Long id;
    private String name;
    private String organizationCode;
    private Boolean main;
    private List<GroupTreeDto> groups;
    private List<OrganizationTreeDto> children;
}
