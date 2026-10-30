/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
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

    /**
     * When true and static approver orgs are configured, only those orgs are approvers (context org
     * and dynamic orgs from the handler are omitted).
     */
    @Column(name = "static_approvers_exclusive", nullable = false)
    @Builder.Default
    private boolean staticApproversExclusive = false;

    /**
     * When true, the opener may claim/decide approver slots on their own workflow if they hold the
     * rights.
     */
    @Column(name = "self_approve_allowed", nullable = false)
    @Builder.Default
    private boolean selfApproveAllowed = false;

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

    @OneToMany(mappedBy = "definition", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkflowDefinitionStaticOrg> staticOrganizations = new ArrayList<>();
}
