/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.standard;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

/**
 * Standard-library template for a workflow definition. {@code config_key} is the workflow type id.
 * Materialized into a per-org {@code wf_definitions} row (+ static approver orgs).
 */
@Entity
@Table(
        name = "std_workflow_definition_templates",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"tenant_id", "standard_id", "config_key"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class WorkflowDefinitionTemplate {

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

    @Column(name = "static_approvers_exclusive", nullable = false)
    @Builder.Default
    private boolean staticApproversExclusive = false;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkflowDefinitionTemplateStaticOrg> staticOrganizations = new ArrayList<>();
}
