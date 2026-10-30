/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import com.emberone.federalstate.FederalStateService;
import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.dto.TenantWorkflowDefinitionDto;
import com.emberone.workflow.dto.UpdateWorkflowDefinitionRequest;
import com.emberone.workflow.exception.WorkflowConfigurationException;
import com.emberone.workflow.repository.WorkflowDefinitionRepository;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tenant-wide workflow settings and per-org materialized rows. Per-standard approver routing lives
 * in the standards admin API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowDefinitionService {

    private final WorkflowDefinitionRepository definitionRepository;
    private final TenantWorkflowDefinitionService tenantWorkflowDefinitionService;
    private final FederalStateService federalStateService;

    @Transactional
    public TenantWorkflowDefinitionDto getTenantDefault(long tenantId, Long workflowTypeId) {
        return withTenantContext(
                tenantId, () -> tenantWorkflowDefinitionService.getDto(tenantId, workflowTypeId));
    }

    @Transactional
    public TenantWorkflowDefinitionDto updateTenantDefault(
            long tenantId, Long workflowTypeId, UpdateWorkflowDefinitionRequest request) {
        return withTenantContext(
                tenantId,
                () ->
                        tenantWorkflowDefinitionService.update(
                                tenantId,
                                workflowTypeId,
                                request.isStaticApproversExclusive(),
                                request.getStaticApproverOrganizationIds()));
    }

    @Transactional
    public TenantWorkflowDefinitionDto restoreTenantDefault(long tenantId, Long workflowTypeId) {
        return withTenantContext(
                tenantId,
                () ->
                        tenantWorkflowDefinitionService.restore(
                                tenantId,
                                workflowTypeId,
                                federalStateService
                                        .settings(tenantId)
                                        .findWorkflowDefinition(workflowTypeId)));
    }

    @Transactional(readOnly = true)
    public WorkflowDefinition resolveEffectiveDefinition(
            Long workflowTypeId, Long contextOrganizationId) {
        return definitionRepository
                .findByOrganizationIdAndWorkflowTypeId(contextOrganizationId, workflowTypeId)
                .orElseThrow(
                        () ->
                                new WorkflowConfigurationException(
                                        "No workflow definition for type id "
                                                + workflowTypeId
                                                + " and organization "
                                                + contextOrganizationId));
    }

    private <T> T withTenantContext(long tenantId, Supplier<T> action) {
        TenantScopeGuard.bind(tenantId);
        TenantScopeGuard.requireActiveTenantMatches(tenantId);
        return action.get();
    }
}
