/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wf_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "workflow_type_id", nullable = false)
    private Long workflowTypeId;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    /** The organization this definition is materialized for. Each org has its own row. */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /**
     * Stable key linking this instance row to the standard template (= workflow type id); null for
     * ad-hoc rows.
     */
    @Column(name = "config_key")
    private String configKey;

    /**
     * Id of the {@code std_workflow_definition_templates} item this row was materialized from; null
     * when the row is no longer tied to a standard template.
     */
    @Column(name = "standard_origin_id")
    private Long standardOriginId;
}
