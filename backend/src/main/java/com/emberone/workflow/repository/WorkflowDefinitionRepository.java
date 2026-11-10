/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.repository;

import com.emberone.workflow.domain.WorkflowDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {

    Optional<WorkflowDefinition> findByOrganizationIdAndWorkflowTypeId(
            Long organizationId, Long workflowTypeId);

    List<WorkflowDefinition> findByWorkflowTypeId(Long workflowTypeId);

    List<WorkflowDefinition> findByOrganizationIdAndStandardOriginIdIsNotNull(Long organizationId);

    Optional<WorkflowDefinition> findByOrganizationIdAndConfigKey(
            Long organizationId, String configKey);
}
