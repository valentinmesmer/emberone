/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.standard;

import jakarta.persistence.*;
import lombok.*;

/** A static approver organization of a {@link WorkflowDefinitionTemplate}. */
@Entity
@Table(
        name = "std_workflow_definition_template_static_orgs",
        uniqueConstraints = {
            @UniqueConstraint(
                    columnNames = {"tenant_id", "template_id", "approver_organization_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowDefinitionTemplateStaticOrg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private WorkflowDefinitionTemplate template;

    @Column(name = "approver_organization_id", nullable = false)
    private Long approverOrganizationId;
}
