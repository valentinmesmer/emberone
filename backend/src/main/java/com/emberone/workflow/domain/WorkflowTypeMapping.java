/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "wf_type_mappings",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"tenant_id", "organization_id", "workflow_type_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTypeMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "workflow_type_id", nullable = false)
    private Long workflowTypeId;

    @Column(name = "organization_group_id")
    private Long organizationGroupId;

    @Column(name = "auto_approve", nullable = false)
    private boolean autoApprove;

    /**
     * Stable key linking this instance row to the standard template (= workflow type id); null for
     * ad-hoc rows.
     */
    @Column(name = "config_key")
    private String configKey;

    /**
     * Id of the {@code std_workflow_type_mapping_templates} item this row was materialized from;
     * null when the row is no longer tied to a standard template.
     */
    @Column(name = "standard_origin_id")
    private Long standardOriginId;
}
