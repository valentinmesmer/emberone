/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/** Tenant-wide workflow settings exposed in Mandant Einstellungen. */
@Value
@Builder
public class TenantWorkflowDefinitionDto {
    Long id;
    Long workflowTypeId;
    boolean staticApproversExclusive;
    List<Long> staticApproverOrganizationIds;
    boolean customized;
}
