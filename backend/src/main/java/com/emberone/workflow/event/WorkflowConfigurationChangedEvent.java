/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.event;

import java.util.Set;

/**
 * Published when workflow approver routing, static approver orgs, or approval flags change. Open
 * workflow instances are reconciled asynchronously after commit.
 */
public record WorkflowConfigurationChangedEvent(
        Long tenantId,
        Long workflowTypeId,
        WorkflowConfigurationScope scope,
        Set<Long> organizationIds) {

    public WorkflowConfigurationChangedEvent {
        if (scope == null) {
            throw new IllegalArgumentException("scope is required");
        }
        organizationIds = organizationIds == null ? Set.of() : Set.copyOf(organizationIds);
        if (scope == WorkflowConfigurationScope.ORGANIZATIONS && organizationIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "organizationIds required when scope is ORGANIZATIONS");
        }
    }

    public static WorkflowConfigurationChangedEvent forOrganizations(
            Long tenantId, Long workflowTypeId, Set<Long> organizationIds) {
        return new WorkflowConfigurationChangedEvent(
                tenantId,
                workflowTypeId,
                WorkflowConfigurationScope.ORGANIZATIONS,
                organizationIds);
    }

    public static WorkflowConfigurationChangedEvent tenantWide(Long tenantId, Long workflowTypeId) {
        return new WorkflowConfigurationChangedEvent(
                tenantId, workflowTypeId, WorkflowConfigurationScope.TENANT_WIDE, Set.of());
    }
}
