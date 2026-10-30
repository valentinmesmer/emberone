/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.dutyfunction.dto;

import com.emberone.organizationgroup.domain.GroupMembershipType;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class OrganizationFunctionOverviewGroupDto {
    String groupKey;
    String functionConfigKey;
    String functionName;
    String functionAbbreviation;
    int functionSortOrder;
    String organizationGroupName;
    String organizationGroupConfigKey;
    GroupMembershipType grantMembershipType;
    List<OrganizationFunctionOverviewMemberDto> members;
}
