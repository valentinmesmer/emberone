/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.repository;

import com.emberone.workflow.domain.WorkflowTypeMapping;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface WorkflowTypeMappingRepository extends JpaRepository<WorkflowTypeMapping, Long> {

    Optional<WorkflowTypeMapping> findByOrganizationIdAndWorkflowTypeId(
            Long organizationId, Long workflowTypeId);

    Optional<WorkflowTypeMapping> findByOrganizationIdAndConfigKey(
            Long organizationId, String configKey);

    List<WorkflowTypeMapping> findByOrganizationIdAndStandardOriginIdIsNotNull(Long organizationId);

    boolean existsByOrganizationIdAndWorkflowTypeId(Long organizationId, Long workflowTypeId);
}
