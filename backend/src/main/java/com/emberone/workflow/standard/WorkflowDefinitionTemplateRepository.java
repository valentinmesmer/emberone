/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.standard;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface WorkflowDefinitionTemplateRepository
        extends JpaRepository<WorkflowDefinitionTemplate, Long> {

    @EntityGraph(attributePaths = "staticOrganizations")
    List<WorkflowDefinitionTemplate> findByStandardId(Long standardId);

    Optional<WorkflowDefinitionTemplate> findByStandardIdAndConfigKey(
            Long standardId, String configKey);
}
