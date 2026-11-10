/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.service;

import com.emberone.workflow.domain.WorkflowTypeMapping;
import com.emberone.workflow.domain.WorkflowTypeMappingSlot;
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
                .orElseThrow(() ->
                        new WorkflowConfigurationException(
                                "Missing workflow type mapping for organization "
                                        + organizationId
                                        + " and type id "
                                        + workflowTypeId));
    }

    /**
     * The settings of one slot role of the org's mapping. A role without a row (a handler gained a
     * role and neither the baseline sync nor a migration added it) is a configuration error, like a
     * missing mapping.
     */
    @Transactional(readOnly = true)
    public WorkflowTypeMappingSlot requireSlot(
            Long organizationId, Long workflowTypeId, String slotRole) {
        WorkflowTypeMapping mapping = requireMapping(organizationId, workflowTypeId);
        return mapping.findSlot(slotRole)
                .orElseThrow(() ->
                        new WorkflowConfigurationException(
                                "Workflow type mapping "
                                        + mapping.getId()
                                        + " has no approver slot settings for role "
                                        + slotRole));
    }
}
