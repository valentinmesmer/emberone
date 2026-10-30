/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.standard;

import com.emberone.federalstate.baseline.WorkflowDefinitionBaseline;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.spi.StandardDomain;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.domain.WorkflowDefinitionStaticOrg;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.repository.WorkflowDefinitionRepository;
import com.emberone.workflow.service.TenantWorkflowDefinitionService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link StandardDomain} for workflow definition templates on org standards. Tenant-wide static
 * approver settings live in {@code tenant_workflow_definitions}; per-org rows in {@code
 * wf_definitions} inherit those settings on materialization.
 */
@Slf4j
@Component
@Order(40)
@RequiredArgsConstructor
public class WorkflowDefinitionStandardDomain implements StandardDomain {

    private final WorkflowDefinitionTemplateRepository templateRepository;
    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowTypeMappingStandardDomain workflowTypeMappingStandardDomain;
    private final TenantWorkflowDefinitionService tenantWorkflowDefinitionService;

    @Override
    public String domainKey() {
        return "workflow-definitions";
    }

    @Override
    @Transactional
    public void copyTemplates(Standard source, Standard target) {
        for (WorkflowDefinitionTemplate template :
                templateRepository.findByStandardId(source.getId())) {
            WorkflowDefinitionTemplate copy =
                    WorkflowDefinitionTemplate.builder()
                            .tenantId(target.getTenantId())
                            .standardId(target.getId())
                            .configKey(template.getConfigKey())
                            .workflowTypeId(template.getWorkflowTypeId())
                            .staticApproversExclusive(template.isStaticApproversExclusive())
                            .staticOrganizations(new ArrayList<>())
                            .build();
            for (WorkflowDefinitionTemplateStaticOrg staticOrg :
                    template.getStaticOrganizations()) {
                copy.getStaticOrganizations()
                        .add(
                                WorkflowDefinitionTemplateStaticOrg.builder()
                                        .tenantId(target.getTenantId())
                                        .template(copy)
                                        .approverOrganizationId(
                                                staticOrg.getApproverOrganizationId())
                                        .build());
            }
            templateRepository.save(copy);
        }
    }

    @Override
    @Transactional
    public void materializeForOrg(Long organizationId, Standard standard) {
        for (WorkflowDefinitionTemplate template :
                templateRepository.findByStandardId(standard.getId())) {
            if (definitionRepository
                    .findByOrganizationIdAndConfigKey(organizationId, template.getConfigKey())
                    .isPresent()) {
                continue;
            }
            definitionRepository.save(toInstance(organizationId, template, standard));
        }
    }

    @Override
    @Transactional
    public void reassignForOrg(Long organizationId, Standard standard) {
        List<WorkflowDefinitionTemplate> templates =
                templateRepository.findByStandardId(standard.getId());
        Set<String> templateKeys =
                templates.stream()
                        .map(WorkflowDefinitionTemplate::getConfigKey)
                        .collect(Collectors.toSet());

        for (WorkflowDefinitionTemplate template : templates) {
            definitionRepository
                    .findByOrganizationIdAndConfigKey(organizationId, template.getConfigKey())
                    .ifPresentOrElse(
                            existing -> applyTemplateToInstance(existing, template),
                            () ->
                                    definitionRepository.save(
                                            toInstance(organizationId, template, standard)));
        }

        for (WorkflowDefinition definition :
                definitionRepository.findByOrganizationIdAndStandardOriginIdIsNotNull(
                        organizationId)) {
            if (definition.getConfigKey() == null
                    || templateKeys.contains(definition.getConfigKey())) {
                continue;
            }
            definitionRepository.delete(definition);
        }
    }

    /**
     * Inserts missing workflow definition templates from the tenant baseline (idempotent).
     *
     * @return number of newly inserted templates
     */
    @Transactional
    public int ensureMissingFromBaseline(
            Standard standard, List<WorkflowDefinitionBaseline> baselineItems) {
        if (baselineItems == null || baselineItems.isEmpty()) {
            return 0;
        }
        int added = 0;
        for (WorkflowDefinitionBaseline item : baselineItems) {
            if (item == null || item.configKey() == null) {
                continue;
            }
            if (!WorkflowType.isKnownId(item.workflowTypeId())) {
                continue;
            }
            if (templateRepository
                    .findByStandardIdAndConfigKey(standard.getId(), item.configKey())
                    .isPresent()) {
                continue;
            }
            templateRepository.save(
                    WorkflowDefinitionTemplate.builder()
                            .tenantId(standard.getTenantId())
                            .standardId(standard.getId())
                            .configKey(item.configKey())
                            .workflowTypeId(item.workflowTypeId())
                            .staticApproversExclusive(item.staticApproversExclusive())
                            .staticOrganizations(new ArrayList<>())
                            .build());
            added++;
            log.info(
                    "Added workflow definition template '{}' to standard {} (id={})",
                    item.configKey(),
                    standard.getName(),
                    standard.getId());
        }
        return added;
    }

    private WorkflowDefinition toInstance(
            Long organizationId, WorkflowDefinitionTemplate template, Standard standard) {
        boolean enabled = true;
        boolean selfApproveAllowed = false;
        Optional<WorkflowTypeMappingTemplate> mappingTemplate =
                workflowTypeMappingStandardDomain.findTemplate(standard, template.getConfigKey());
        if (mappingTemplate.isPresent()) {
            enabled = mappingTemplate.get().isEnabled();
            selfApproveAllowed = mappingTemplate.get().isSelfApproveAllowed();
        }
        WorkflowDefinition definition =
                WorkflowDefinition.builder()
                        .tenantId(template.getTenantId())
                        .workflowTypeId(template.getWorkflowTypeId())
                        .organizationId(organizationId)
                        .enabled(enabled)
                        .staticApproversExclusive(template.isStaticApproversExclusive())
                        .selfApproveAllowed(selfApproveAllowed)
                        .configKey(template.getConfigKey())
                        .standardOriginId(template.getId())
                        .staticOrganizations(new ArrayList<>())
                        .build();
        applyStaticOrgsFromTemplate(definition, template);
        tenantWorkflowDefinitionService.applyToOrgDefinition(
                template.getWorkflowTypeId(), definition);
        return definition;
    }

    private void applyStaticOrgsFromTemplate(
            WorkflowDefinition definition, WorkflowDefinitionTemplate template) {
        Set<Long> desired =
                template.getStaticOrganizations().stream()
                        .map(WorkflowDefinitionTemplateStaticOrg::getApproverOrganizationId)
                        .collect(Collectors.toSet());
        definition
                .getStaticOrganizations()
                .removeIf(existing -> !desired.contains(existing.getApproverOrganizationId()));
        Set<Long> present =
                definition.getStaticOrganizations().stream()
                        .map(WorkflowDefinitionStaticOrg::getApproverOrganizationId)
                        .collect(Collectors.toSet());
        for (Long orgId : desired) {
            if (!present.contains(orgId)) {
                definition
                        .getStaticOrganizations()
                        .add(
                                WorkflowDefinitionStaticOrg.builder()
                                        .tenantId(definition.getTenantId())
                                        .definition(definition)
                                        .approverOrganizationId(orgId)
                                        .build());
            }
        }
    }

    private void applyTemplateToInstance(
            WorkflowDefinition definition, WorkflowDefinitionTemplate template) {
        tenantWorkflowDefinitionService.applyToOrgDefinition(
                template.getWorkflowTypeId(), definition);
        definition.setStandardOriginId(template.getId());
        definitionRepository.save(definition);
    }
}
