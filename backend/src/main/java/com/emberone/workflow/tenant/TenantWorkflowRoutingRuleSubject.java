/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.tenant;

import jakarta.persistence.*;
import lombok.*;

/** A subtype id (catalog id of the workflow's routing dimension) covered by a routing rule. */
@Entity
@Table(
        name = "tenant_workflow_routing_rule_subjects",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"tenant_id", "definition_id", "subject_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantWorkflowRoutingRuleSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "definition_id", nullable = false)
    private Long definitionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private TenantWorkflowRoutingRule rule;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;
}
