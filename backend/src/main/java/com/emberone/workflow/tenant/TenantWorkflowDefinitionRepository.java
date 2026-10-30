/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.tenant;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface TenantWorkflowDefinitionRepository
        extends JpaRepository<TenantWorkflowDefinition, Long> {

    Optional<TenantWorkflowDefinition> findByWorkflowTypeId(Long workflowTypeId);
}
