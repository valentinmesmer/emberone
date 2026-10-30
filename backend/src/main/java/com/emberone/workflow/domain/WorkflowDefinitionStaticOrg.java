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
        name = "wf_definition_static_orgs",
        uniqueConstraints = {
            @UniqueConstraint(
                    columnNames = {
                        "tenant_id",
                        "workflow_definition_id",
                        "approver_organization_id"
                    })
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowDefinitionStaticOrg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_definition_id", nullable = false)
    private WorkflowDefinition definition;

    @Column(name = "approver_organization_id", nullable = false)
    private Long approverOrganizationId;
}
