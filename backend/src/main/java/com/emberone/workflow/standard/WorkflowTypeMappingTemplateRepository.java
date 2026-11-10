/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.standard;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface WorkflowTypeMappingTemplateRepository
        extends JpaRepository<WorkflowTypeMappingTemplate, Long> {

    List<WorkflowTypeMappingTemplate> findByStandardId(Long standardId);

    Optional<WorkflowTypeMappingTemplate> findByStandardIdAndConfigKey(
            Long standardId, String configKey);

    /** Whether any slot of any template in the standard uses the org group as its approver. */
    @Query(
            """
            SELECT COUNT(s) > 0 FROM WorkflowTypeMappingTemplateSlot s
            WHERE s.template.standardId = :standardId
              AND s.orgGroupConfigKey = :orgGroupConfigKey
            """)
    boolean existsByStandardIdAndSlotOrgGroupConfigKey(
            @Param("standardId") Long standardId,
            @Param("orgGroupConfigKey") String orgGroupConfigKey);
}
