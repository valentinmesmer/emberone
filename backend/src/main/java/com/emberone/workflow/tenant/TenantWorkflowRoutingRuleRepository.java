/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.tenant;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface TenantWorkflowRoutingRuleRepository
        extends JpaRepository<TenantWorkflowRoutingRule, Long> {

    @EntityGraph(attributePaths = {"subjects", "organizations"})
    List<TenantWorkflowRoutingRule> findByDefinitionIdOrderByIdAsc(Long definitionId);

    @EntityGraph(attributePaths = {"subjects", "organizations"})
    Optional<TenantWorkflowRoutingRule> findByIdAndDefinitionId(Long id, Long definitionId);
}
