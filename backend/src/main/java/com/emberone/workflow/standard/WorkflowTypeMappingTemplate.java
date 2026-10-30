/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.standard;

import jakarta.persistence.*;
import lombok.*;

/**
 * Standard-library template for a workflow type mapping (which org group approves a workflow type).
 * The approver group is referenced by {@code org_group_config_key} (e.g. LEADERSHIP) and resolved
 * to a concrete per-org group at materialization time. {@code config_key} is the workflow type id.
 */
@Entity
@Table(
        name = "std_workflow_type_mapping_templates",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"tenant_id", "standard_id", "config_key"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class WorkflowTypeMappingTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "standard_id", nullable = false)
    private Long standardId;

    @Column(name = "config_key", nullable = false)
    private String configKey;

    @Column(name = "workflow_type_id", nullable = false)
    private Long workflowTypeId;

    @Column(name = "org_group_config_key", nullable = false)
    private String orgGroupConfigKey;

    @Column(name = "auto_approve", nullable = false)
    private boolean autoApprove;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "self_approve_allowed", nullable = false)
    @Builder.Default
    private boolean selfApproveAllowed = false;
}
