/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.standard;

import com.emberone.federalstate.baseline.WorkflowDefinitionBaseline;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.spi.StandardDomain;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.repository.WorkflowDefinitionRepository;
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
 * {@link StandardDomain} for workflow definition templates on org standards. Tenant-wide extra
 * approver orgs live only in {@code tenant_workflow_definitions} (and its routing rules); per-org
 * rows in {@code wf_definitions} carry no copy of them.
 */
@Slf4j
@Component
@Order(40)
@RequiredArgsConstructor
public class WorkflowDefinitionStandardDomain implements StandardDomain {

    private final WorkflowDefinitionTemplateRepository templateRepository;
    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowTypeMappingStandardDomain workflowTypeMappingStandardDomain;

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
                            .build();
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
        Optional<WorkflowTypeMappingTemplate> mappingTemplate =
                workflowTypeMappingStandardDomain.findTemplate(standard, template.getConfigKey());
        if (mappingTemplate.isPresent()) {
            enabled = mappingTemplate.get().isEnabled();
        }
        WorkflowDefinition definition =
                WorkflowDefinition.builder()
                        .tenantId(template.getTenantId())
                        .workflowTypeId(template.getWorkflowTypeId())
                        .organizationId(organizationId)
                        .enabled(enabled)
                        .configKey(template.getConfigKey())
                        .standardOriginId(template.getId())
                        .build();
        return definition;
    }

    private void applyTemplateToInstance(
            WorkflowDefinition definition, WorkflowDefinitionTemplate template) {
        definition.setStandardOriginId(template.getId());
        definitionRepository.save(definition);
    }
}
