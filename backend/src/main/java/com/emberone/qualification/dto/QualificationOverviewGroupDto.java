/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
