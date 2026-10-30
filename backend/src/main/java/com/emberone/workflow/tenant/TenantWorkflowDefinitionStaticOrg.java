/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.tenant;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "tenant_workflow_definition_static_orgs",
        uniqueConstraints = {
            @UniqueConstraint(
                    columnNames = {"tenant_id", "definition_id", "approver_organization_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantWorkflowDefinitionStaticOrg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "definition_id", nullable = false)
    private TenantWorkflowDefinition definition;

    @Column(name = "approver_organization_id", nullable = false)
    private Long approverOrganizationId;
}
