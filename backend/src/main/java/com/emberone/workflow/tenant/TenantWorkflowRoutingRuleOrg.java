/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.tenant;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "tenant_workflow_routing_rule_orgs",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"tenant_id", "rule_id", "approver_organization_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantWorkflowRoutingRuleOrg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private TenantWorkflowRoutingRule rule;

    @Column(name = "approver_organization_id", nullable = false)
    private Long approverOrganizationId;
}
