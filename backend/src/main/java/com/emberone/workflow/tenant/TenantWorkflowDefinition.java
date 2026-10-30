/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.tenant;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

/**
 * Tenant-wide workflow settings (Mandant Einstellungen). Separate from read-only {@code
 * std_workflow_definition_templates} on the system standard; materialized into per-org {@code
 * wf_definitions} rows.
 */
@Entity
@Table(
        name = "tenant_workflow_definitions",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"tenant_id", "workflow_type_id"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class TenantWorkflowDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "workflow_type_id", nullable = false)
    private Long workflowTypeId;

    @Column(name = "static_approvers_exclusive", nullable = false)
    @Builder.Default
    private boolean staticApproversExclusive = false;

    /** True when the tenant changed settings away from the system baseline (restorable). */
    @Column(name = "customized", nullable = false)
    @Builder.Default
    private boolean customized = false;

    @OneToMany(mappedBy = "definition", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TenantWorkflowDefinitionStaticOrg> staticOrganizations = new ArrayList<>();
}
