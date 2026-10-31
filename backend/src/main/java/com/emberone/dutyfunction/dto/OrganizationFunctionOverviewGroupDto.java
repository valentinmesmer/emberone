/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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
