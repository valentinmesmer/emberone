/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.organizationgroup.dto;

import com.emberone.organizationgroup.domain.OrganizationGroupType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationGroupDto {
    private Long id;
    private Long organizationId;
    private String name;
    private String description;
    private OrganizationGroupType type;
    private boolean archived;
    private Instant archivedAt;
}
