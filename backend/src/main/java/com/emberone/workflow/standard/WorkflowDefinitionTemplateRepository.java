/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.standard;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface WorkflowDefinitionTemplateRepository
        extends JpaRepository<WorkflowDefinitionTemplate, Long> {

    List<WorkflowDefinitionTemplate> findByStandardId(Long standardId);

    Optional<WorkflowDefinitionTemplate> findByStandardIdAndConfigKey(
            Long standardId, String configKey);
}
