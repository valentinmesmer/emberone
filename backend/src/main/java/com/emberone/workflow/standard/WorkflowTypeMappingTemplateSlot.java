/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.standard;

import jakarta.persistence.*;
import lombok.*;

/**
 * Standard-library settings of one slot role of a {@link WorkflowTypeMappingTemplate}. The approver
 * group is referenced by {@code org_group_config_key} (e.g. LEADERSHIP) and resolved to a concrete
 * per-org group at materialization time.
 */
@Entity
@Table(
        name = "std_workflow_type_mapping_template_slots",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"tenant_id", "template_id", "slot_role"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTypeMappingTemplateSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private WorkflowTypeMappingTemplate template;

    @Column(name = "slot_role", nullable = false, length = 64)
    private String slotRole;

    @Column(name = "org_group_config_key", nullable = false)
    private String orgGroupConfigKey;

    @Column(name = "auto_approve", nullable = false)
    private boolean autoApprove;

    @Column(name = "self_approve_allowed", nullable = false)
    private boolean selfApproveAllowed;
}
