/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.service;

import com.emberone.federalstate.FederalStateService;
import com.emberone.federalstate.baseline.WorkflowDefinitionBaseline;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.workflow.dto.SaveWorkflowRoutingRuleRequest;
import com.emberone.workflow.dto.TenantWorkflowDefinitionDto;
import com.emberone.workflow.dto.WorkflowRoutingRuleDto;
import com.emberone.workflow.event.WorkflowConfigurationChangedEvent;
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
    private final TenantWorkflowRoutingRuleService routingRuleService;
    private final OrganizationRepository organizationRepository;
    private final FederalStateService federalStateService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TenantWorkflowDefinitionDto getDto(long tenantId, Long workflowTypeId) {
        return withTenantContext(
                tenantId, () -> toDto(requireDefinition(tenantId, workflowTypeId)));
    }

    /** All tenant default definitions (one per baseline workflow type), sorted by type id. */
    @Transactional
    public List<TenantWorkflowDefinitionDto> listDtos(long tenantId) {
        return withTenantContext(
                tenantId,
                () ->
                        federalStateService
                                .settings(tenantId)
                                .tenantBaseline()
                                .workflowDefinitions()
                                .stream()
                                .map(WorkflowDefinitionBaseline::workflowTypeId)
                                .sorted()
                                .map(workflowTypeId ->
                                        toDto(requireDefinition(tenantId, workflowTypeId)))
                                .toList());
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
                    routingRuleService.deleteAll(definition);
                    TenantWorkflowDefinition saved = definitionRepository.save(definition);
                    publishConfigurationChanged(tenantId, workflowTypeId);
                    log.info("Restored tenant workflow definition for type id {}", workflowTypeId);
                    return toDto(saved);
                });
    }

    @Transactional
    public WorkflowRoutingRuleDto getRoutingRule(long tenantId, Long workflowTypeId, Long ruleId) {
        return withTenantContext(
                tenantId,
                () -> routingRuleService.get(requireDefinition(tenantId, workflowTypeId), ruleId));
    }

    @Transactional
    public WorkflowRoutingRuleDto createRoutingRule(
            long tenantId, Long workflowTypeId, SaveWorkflowRoutingRuleRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    TenantWorkflowDefinition definition =
                            requireDefinition(tenantId, workflowTypeId);
                    WorkflowRoutingRuleDto rule = routingRuleService.create(definition, request);
                    afterRuleChange(tenantId, workflowTypeId, definition);
                    return rule;
                });
    }

    @Transactional
    public WorkflowRoutingRuleDto updateRoutingRule(
            long tenantId,
            Long workflowTypeId,
            Long ruleId,
            SaveWorkflowRoutingRuleRequest request) {
        return withTenantContext(
                tenantId,
                () -> {
                    TenantWorkflowDefinition definition =
                            requireDefinition(tenantId, workflowTypeId);
                    WorkflowRoutingRuleDto rule =
                            routingRuleService.update(definition, ruleId, request);
                    afterRuleChange(tenantId, workflowTypeId, definition);
                    return rule;
                });
    }

    @Transactional
    public void deleteRoutingRule(long tenantId, Long workflowTypeId, Long ruleId) {
        withTenantContext(
                tenantId,
                () -> {
                    TenantWorkflowDefinition definition =
                            requireDefinition(tenantId, workflowTypeId);
                    routingRuleService.delete(definition, ruleId);
                    afterRuleChange(tenantId, workflowTypeId, definition);
                    return null;
                });
    }

    private void afterRuleChange(
            long tenantId, Long workflowTypeId, TenantWorkflowDefinition definition) {
        definition.setCustomized(true);
        definitionRepository.save(definition);
        publishConfigurationChanged(tenantId, workflowTypeId);
        log.info("Changed routing rules of tenant workflow definition {}", workflowTypeId);
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
                                        .orElseGet(() ->
                                                TenantWorkflowDefinition.builder()
                                                        .tenantId(tenantId)
                                                        .workflowTypeId(item.workflowTypeId())
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
                .routingRules(routingRuleService.list(definition))
                .build();
    }

    private <T> T withTenantContext(long tenantId, Supplier<T> action) {
        TenantScopeGuard.bind(tenantId);
        TenantScopeGuard.requireActiveTenantMatches(tenantId);
        return action.get();
    }
}
