/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.qualification.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class QualificationOverviewGroupDto {
    Long tenantQualificationDefinitionId;
    String configKey;
    String name;
    boolean requiresOrgDecision;
    List<QualificationOverviewMemberDto> members;
    List<QualificationOverviewMemberDto> appointableMembers;
}
