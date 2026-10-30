/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
