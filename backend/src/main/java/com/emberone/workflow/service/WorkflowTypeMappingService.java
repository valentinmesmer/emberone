/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import com.emberone.workflow.domain.WorkflowTypeMapping;
import com.emberone.workflow.exception.WorkflowConfigurationException;
import com.emberone.workflow.repository.WorkflowTypeMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkflowTypeMappingService {

    private final WorkflowTypeMappingRepository mappingRepository;

    @Transactional(readOnly = true)
    public WorkflowTypeMapping requireMapping(Long organizationId, Long workflowTypeId) {
        return mappingRepository
                .findByOrganizationIdAndWorkflowTypeId(organizationId, workflowTypeId)
                .orElseThrow(
                        () ->
                                new WorkflowConfigurationException(
                                        "Missing workflow type mapping for organization "
                                                + organizationId
                                                + " and type id "
                                                + workflowTypeId));
    }
}
