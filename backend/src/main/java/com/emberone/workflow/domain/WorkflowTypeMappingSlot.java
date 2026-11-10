/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Approval settings of one slot role ({@code CONTEXT}, {@code ADDITIONAL}, handler roles, see
 * {@link com.emberone.workflow.spi.WorkflowApproverSlotRoles}) of a {@link WorkflowTypeMapping}.
 */
@Entity
@Table(
        name = "wf_type_mapping_slots",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"tenant_id", "mapping_id", "slot_role"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTypeMappingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mapping_id", nullable = false)
    private WorkflowTypeMapping mapping;

    @Column(name = "slot_role", nullable = false, length = 64)
    private String slotRole;

    @Column(name = "organization_group_id", nullable = false)
    private Long organizationGroupId;

    @Column(name = "auto_approve", nullable = false)
    private boolean autoApprove;

    /**
     * When true, the opener may claim/decide an approver slot of this role on their own workflow if
     * they hold the rights.
     */
    @Column(name = "self_approve_allowed", nullable = false)
    private boolean selfApproveAllowed;
}
