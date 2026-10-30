/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import com.emberone.federalstate.FederalStateService;
import com.emberone.federalstate.baseline.WorkflowDefinitionBaseline;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.domain.WorkflowDefinitionStaticOrg;
import com.emberone.workflow.dto.TenantWorkflowDefinitionDto;
import com.emberone.workflow.event.WorkflowConfigurationChangedEvent;
import com.emberone.workflow.repository.WorkflowDefinitionRepository;
import com.emberone.workflow.tenant.TenantWorkflowDefinition;
import com.emberone.workflow.tenant.TenantWorkflowDefinitionRepository;
import com.emberone.workflow.tenant.TenantWorkflowDefinitionStaticOrg;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantWorkflowDefinitionService {

    private final TenantWorkflowDefinitionRepository definitionRepository;
    private final WorkflowDefinitionRepository orgDefinitionRepository;
    private final OrganizationRepository organizationRepository;
    private final FederalStateService federalStateService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TenantWorkflowDefinitionDto getDto(long tenantId, Long workflowTypeId) {
        return withTenantContext(
                tenantId, () -> toDto(requireDefinition(tenantId, workflowTypeId)));
    }

    @Transactional
    public TenantWorkflowDefinitionDto update(
            long tenantId,
            Long workflowTypeId,
            boolean staticApproversExclusive,
            List<Long> staticOrgIds) {
        return withTenantContext(
                tenantId,
                () -> {
                    TenantWorkflowDefinition definition =
                            requireDefinition(tenantId, workflowTypeId);
                    definition.setStaticApproversExclusive(staticApproversExclusive);
                    applyStaticOrgs(definition, staticOrgIds == null ? List.of() : staticOrgIds);
                    definition.setCustomized(true);
                    TenantWorkflowDefinition saved = definitionRepository.save(definition);
                    propagateToOrgDefinitions(workflowTypeId, saved);
                    publishConfigurationChanged(tenantId, workflowTypeId);
                    log.info("Updated tenant workflow definition for type id {}", workflowTypeId);
                    return toDto(saved);
                });
    }

    @Transactional
    public TenantWorkflowDefinitionDto restore(
            long tenantId, Long workflowTypeId, WorkflowDefinitionBaseline baseline) {
        return withTenantContext(
                tenantId,
                () -> {
                    if (baseline == null) {
                        throw new ResourceNotFoundException(
                                "System baseline workflow definition not found: " + workflowTypeId);
                    }
                    TenantWorkflowDefinition definition =
                            requireDefinition(tenantId, workflowTypeId);
                    definition.setStaticApproversExclusive(baseline.staticApproversExclusive());
                    applyStaticOrgs(definition, List.of());
                    definition.setCustomized(false);
                    TenantWorkflowDefinition saved = definitionRepository.save(definition);
                    propagateToOrgDefinitions(workflowTypeId, saved);
                    publishConfigurationChanged(tenantId, workflowTypeId);
                    log.info("Restored tenant workflow definition for type id {}", workflowTypeId);
                    return toDto(saved);
                });
    }

    @Transactional
    public void syncFromBaseline(long tenantId, List<WorkflowDefinitionBaseline> items) {
        withTenantContext(
                tenantId,
                () -> {
                    for (WorkflowDefinitionBaseline item : items) {
                        TenantWorkflowDefinition definition =
                                definitionRepository
                                        .findByWorkflowTypeId(item.workflowTypeId())
                                        .orElseGet(
                                                () ->
                                                        TenantWorkflowDefinition.builder()
                                                                .tenantId(tenantId)
                                                                .workflowTypeId(
                                                                        item.workflowTypeId())
                                                                .build());
                        if (!definition.isCustomized()) {
                            definition.setStaticApproversExclusive(item.staticApproversExclusive());
                            applyStaticOrgs(definition, List.of());
                        }
                        definitionRepository.save(definition);
                    }
                    return null;
                });
    }

    @Transactional
    public TenantWorkflowDefinition requireDefinition(long tenantId, Long workflowTypeId) {
        return definitionRepository
                .findByWorkflowTypeId(workflowTypeId)
                .orElseGet(() -> createFromBaseline(tenantId, workflowTypeId));
    }

    public void applyToOrgDefinition(
            WorkflowDefinition orgDefinition, TenantWorkflowDefinition tenantDefinition) {
        orgDefinition.setStaticApproversExclusive(tenantDefinition.isStaticApproversExclusive());
        applyStaticOrgs(orgDefinition, tenantDefinition);
    }

    public void applyToOrgDefinition(Long workflowTypeId, WorkflowDefinition orgDefinition) {
        definitionRepository
                .findByWorkflowTypeId(workflowTypeId)
                .ifPresent(definition -> applyToOrgDefinition(orgDefinition, definition));
    }

    private TenantWorkflowDefinition createFromBaseline(long tenantId, Long workflowTypeId) {
        WorkflowDefinitionBaseline baseline =
                federalStateService.settings(tenantId).findWorkflowDefinition(workflowTypeId);
        if (baseline == null) {
            throw new ResourceNotFoundException(
                    "Tenant workflow definition not found for type id " + workflowTypeId);
        }
        TenantWorkflowDefinition definition =
                TenantWorkflowDefinition.builder()
                        .tenantId(tenantId)
                        .workflowTypeId(workflowTypeId)
                        .staticApproversExclusive(baseline.staticApproversExclusive())
                        .customized(false)
                        .build();
        return definitionRepository.save(definition);
    }

    private void propagateToOrgDefinitions(
            Long workflowTypeId, TenantWorkflowDefinition tenantDefinition) {
        for (WorkflowDefinition orgDefinition :
                orgDefinitionRepository.findByWorkflowTypeId(workflowTypeId)) {
            applyToOrgDefinition(orgDefinition, tenantDefinition);
            orgDefinitionRepository.save(orgDefinition);
        }
    }

    private void publishConfigurationChanged(long tenantId, Long workflowTypeId) {
        eventPublisher.publishEvent(
                WorkflowConfigurationChangedEvent.tenantWide(tenantId, workflowTypeId));
    }

    private void applyStaticOrgs(TenantWorkflowDefinition definition, List<Long> staticOrgIds) {
        Set<Long> desired = new LinkedHashSet<>(staticOrgIds);
        validateStaticOrgsBelongToTenant(definition.getTenantId(), desired);
        definition
                .getStaticOrganizations()
                .removeIf(existing -> !desired.contains(existing.getApproverOrganizationId()));
        Set<Long> present =
                definition.getStaticOrganizations().stream()
                        .map(TenantWorkflowDefinitionStaticOrg::getApproverOrganizationId)
                        .collect(Collectors.toSet());
        for (Long orgId : desired) {
            if (!present.contains(orgId)) {
                definition
                        .getStaticOrganizations()
                        .add(
                                TenantWorkflowDefinitionStaticOrg.builder()
                                        .tenantId(definition.getTenantId())
                                        .definition(definition)
                                        .approverOrganizationId(orgId)
                                        .build());
            }
        }
    }

    private void validateStaticOrgsBelongToTenant(Long tenantId, Set<Long> organizationIds) {
        for (Long organizationId : organizationIds) {
            if (!organizationRepository.existsByIdAndTenantId(organizationId, tenantId)) {
                throw new IllegalArgumentException(
                        "Organization " + organizationId + " does not belong to the tenant");
            }
        }
    }

    private void applyStaticOrgs(
            WorkflowDefinition orgDefinition, TenantWorkflowDefinition tenantDefinition) {
        Set<Long> desired =
                tenantDefinition.getStaticOrganizations().stream()
                        .map(TenantWorkflowDefinitionStaticOrg::getApproverOrganizationId)
                        .collect(Collectors.toSet());
        orgDefinition
                .getStaticOrganizations()
                .removeIf(existing -> !desired.contains(existing.getApproverOrganizationId()));
        Set<Long> present =
                orgDefinition.getStaticOrganizations().stream()
                        .map(WorkflowDefinitionStaticOrg::getApproverOrganizationId)
                        .collect(Collectors.toSet());
        for (Long orgId : desired) {
            if (!present.contains(orgId)) {
                orgDefinition
                        .getStaticOrganizations()
                        .add(
                                WorkflowDefinitionStaticOrg.builder()
                                        .tenantId(orgDefinition.getTenantId())
                                        .definition(orgDefinition)
                                        .approverOrganizationId(orgId)
                                        .build());
            }
        }
    }

    private TenantWorkflowDefinitionDto toDto(TenantWorkflowDefinition definition) {
        List<Long> staticOrgIds =
                definition.getStaticOrganizations().stream()
                        .map(TenantWorkflowDefinitionStaticOrg::getApproverOrganizationId)
                        .sorted(Comparator.naturalOrder())
                        .toList();
        return TenantWorkflowDefinitionDto.builder()
                .id(definition.getId())
                .workflowTypeId(definition.getWorkflowTypeId())
                .staticApproversExclusive(definition.isStaticApproversExclusive())
                .staticApproverOrganizationIds(staticOrgIds)
                .customized(definition.isCustomized())
                .build();
    }

    private <T> T withTenantContext(long tenantId, Supplier<T> action) {
        TenantScopeGuard.bind(tenantId);
        TenantScopeGuard.requireActiveTenantMatches(tenantId);
        return action.get();
    }
}
