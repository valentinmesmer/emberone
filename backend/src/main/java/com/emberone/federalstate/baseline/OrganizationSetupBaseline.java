/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.federalstate.baseline;

import java.util.List;

/** Org-setup templates for a federal-state SYSTEM standard (no tenant or database ids). */
public record OrganizationSetupBaseline(
        List<OrgGroupBaseline> organizationGroups,
        List<PermissionGroupBaseline> permissionGroups,
        List<GroupMappingBaseline> groupMappings,
        List<WorkflowTypeMappingBaseline> workflowTypeMappings) {

    public OrganizationSetupBaseline {
        organizationGroups =
                organizationGroups == null ? List.of() : List.copyOf(organizationGroups);
        permissionGroups = permissionGroups == null ? List.of() : List.copyOf(permissionGroups);
        groupMappings = groupMappings == null ? List.of() : List.copyOf(groupMappings);
        workflowTypeMappings =
                workflowTypeMappings == null ? List.of() : List.copyOf(workflowTypeMappings);
    }
}
