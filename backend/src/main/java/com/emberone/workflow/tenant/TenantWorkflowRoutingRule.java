/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.tenant;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.*;

/**
 * Tenant-wide approver routing for a set of subtypes (e.g. certifications) of one workflow type.
 * Subtypes without a rule use the default rule on {@link TenantWorkflowDefinition}.
 */
@Entity
@Table(name = "tenant_workflow_routing_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantWorkflowRoutingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "definition_id", nullable = false)
    private TenantWorkflowDefinition definition;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TenantWorkflowRoutingRuleSubject> subjects = new LinkedHashSet<>();

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TenantWorkflowRoutingRuleOrg> organizations = new LinkedHashSet<>();
}
