/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.event;

/** Which organizations were affected by an admin workflow configuration change. */
public enum WorkflowConfigurationScope {
    /** Mapping or definition rows changed for specific organizations. */
    ORGANIZATIONS,
    /**
     * Tenant-wide workflow definition (e.g. static approver orgs); reconcile all open instances of
     * the type.
     */
    TENANT_WIDE
}
