/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.standard;

import com.emberone.federalstate.baseline.WorkflowTypeMappingBaseline;
import com.emberone.organization.domain.Organization;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.organizationgroup.standard.OrgGroupTemplateRepository;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.spi.StandardContentItem;
import com.emberone.standard.spi.StandardDomain;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.domain.WorkflowTypeMapping;
import com.emberone.workflow.event.WorkflowConfigurationChangedEvent;
import com.emberone.workflow.repository.WorkflowDefinitionRepository;
import com.emberone.workflow.repository.WorkflowTypeMappingRepository;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link StandardDomain} for workflow type mappings (which org group approves a workflow type).
 * Templates: {@code std_workflow_type_mapping_templates}; instances: {@code wf_type_mappings}. Runs
 * after the organization-groups domain (it resolves the approver group per org).
 */
@Slf4j
@Component
@Order(30)
@RequiredArgsConstructor
public class WorkflowTypeMappingStandardDomain implements StandardDomain {

    private final WorkflowTypeMappingTemplateRepository templateRepository;
    private final WorkflowTypeMappingRepository mappingRepository;
    private final WorkflowDefinitionRepository definitionRepository;
    private final OrganizationGroupRepository organizationGroupRepository;
    private final OrgGroupTemplateRepository orgGroupTemplateRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public String domainKey() {
        return "workflow-type-mappings";
    }

    @Override
    @Transactional
    public void copyTemplates(Standard source, Standard target) {
        for (WorkflowTypeMappingTemplate template :
                templateRepository.findByStandardId(source.getId())) {
            templateRepository.save(
                    template.toBuilder()
                            .id(null)
                            .tenantId(target.getTenantId())
                            .standardId(target.getId())
                            .build());
        }
    }

    @Override
    @Transactional
    public void materializeForOrg(Long organizationId, Standard standard) {
        for (WorkflowTypeMappingTemplate template :
                templateRepository.findByStandardId(standard.getId())) {
            if (mappingRepository
                    .findByOrganizationIdAndConfigKey(organizationId, template.getConfigKey())
                    .isPresent()) {
                continue;
            }
            toInstance(organizationId, template).ifPresent(mappingRepository::save);
            syncDefinitionFlags(organizationId, template);
        }
    }

    @Override
    @Transactional
    public void reassignForOrg(Long organizationId, Standard standard) {
        List<WorkflowTypeMappingTemplate> templates =
                templateRepository.findByStandardId(standard.getId());
        Set<String> templateKeys =
                templates.stream()
                        .map(WorkflowTypeMappingTemplate::getConfigKey)
                        .collect(Collectors.toSet());
        Set<Long> affectedWorkflowTypeIds = new LinkedHashSet<>();

        for (WorkflowTypeMappingTemplate template : templates) {
            if (WorkflowType.isKnownId(template.getWorkflowTypeId())) {
                affectedWorkflowTypeIds.add(template.getWorkflowTypeId());
            }
            mappingRepository
                    .findByOrganizationIdAndConfigKey(organizationId, template.getConfigKey())
                    .ifPresentOrElse(
                            existing -> applyTemplateToInstance(organizationId, existing, template),
                            () ->
                                    toInstance(organizationId, template)
                                            .ifPresent(mappingRepository::save));
            syncDefinitionFlags(organizationId, template);
        }

        for (WorkflowTypeMapping mapping :
                mappingRepository.findByOrganizationIdAndStandardOriginIdIsNotNull(
                        organizationId)) {
            if (mapping.getConfigKey() == null || templateKeys.contains(mapping.getConfigKey())) {
                continue;
            }
            if (WorkflowType.isKnownId(mapping.getWorkflowTypeId())) {
                affectedWorkflowTypeIds.add(mapping.getWorkflowTypeId());
            }
            mappingRepository.delete(mapping);
        }

        publishConfigurationChangedForOrg(standard, organizationId, affectedWorkflowTypeIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StandardContentItem> describeContent(Standard standard) {
        return templateRepository.findByStandardId(standard.getId()).stream()
                .filter(template -> WorkflowType.isKnownId(template.getWorkflowTypeId()))
                .sorted(
                        Comparator.comparing(
                                        (WorkflowTypeMappingTemplate template) ->
                                                WorkflowType.fromId(template.getWorkflowTypeId())
                                                        .openScope())
                                .thenComparingLong(WorkflowTypeMappingTemplate::getWorkflowTypeId))
                .map(
                        template -> {
                            WorkflowType workflowType =
                                    WorkflowType.fromId(template.getWorkflowTypeId());
                            Map<String, String> attributes = new java.util.HashMap<>();
                            attributes.put("openScope", workflowType.openScope().name());
                            attributes.put(
                                    "approverOrgGroupConfigKey", template.getOrgGroupConfigKey());
                            attributes.put("autoApprove", String.valueOf(template.isAutoApprove()));
                            attributes.put("enabled", String.valueOf(template.isEnabled()));
                            attributes.put(
                                    "selfApproveAllowed",
                                    String.valueOf(template.isSelfApproveAllowed()));
                            return new StandardContentItem(
                                    domainKey(),
                                    template.getConfigKey(),
                                    workflowType.name(),
                                    template.getOrgGroupConfigKey(),
                                    List.of(),
                                    attributes,
                                    true);
                        })
                .toList();
    }

    /**
     * Update approver routing and workflow flags on a mapping template and propagate to
     * materialized rows.
     */
    @Transactional
    public void updateMappingTemplate(
            Standard standard,
            String configKey,
            String orgGroupConfigKey,
            boolean autoApprove,
            boolean enabled,
            boolean selfApproveAllowed,
            List<Organization> orgs) {
        WorkflowTypeMappingTemplate template =
                templateRepository
                        .findByStandardIdAndConfigKey(standard.getId(), configKey)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Workflow mapping '"
                                                        + configKey
                                                        + "' not found in standard "
                                                        + standard.getId()));
        template.setOrgGroupConfigKey(orgGroupConfigKey);
        template.setAutoApprove(autoApprove);
        template.setEnabled(enabled);
        template.setSelfApproveAllowed(selfApproveAllowed);
        templateRepository.save(template);
        Set<Long> organizationIds = new java.util.LinkedHashSet<>();
        for (Organization org : orgs) {
            mappingRepository
                    .findByOrganizationIdAndConfigKey(org.getId(), configKey)
                    .ifPresent(
                            existing -> applyTemplateToInstance(org.getId(), existing, template));
            syncDefinitionFlags(org.getId(), template);
            organizationIds.add(org.getId());
        }
        if (!organizationIds.isEmpty()) {
            publishConfigurationChangedForOrg(
                    standard, organizationIds, Set.of(template.getWorkflowTypeId()));
        }
    }

    private void publishConfigurationChangedForOrg(
            Standard standard, Long organizationId, Set<Long> workflowTypeIds) {
        publishConfigurationChangedForOrg(standard, Set.of(organizationId), workflowTypeIds);
    }

    private void publishConfigurationChangedForOrg(
            Standard standard, Set<Long> organizationIds, Set<Long> workflowTypeIds) {
        if (standard == null
                || organizationIds == null
                || organizationIds.isEmpty()
                || workflowTypeIds == null
                || workflowTypeIds.isEmpty()) {
            return;
        }
        Long tenantId = standard.getTenantId();
        for (Long workflowTypeId : workflowTypeIds) {
            if (!WorkflowType.isKnownId(workflowTypeId)) {
                continue;
            }
            eventPublisher.publishEvent(
                    WorkflowConfigurationChangedEvent.forOrganizations(
                            tenantId, workflowTypeId, organizationIds));
        }
    }

    @Transactional(readOnly = true)
    public Optional<WorkflowTypeMappingTemplate> findTemplate(Standard standard, String configKey) {
        return templateRepository.findByStandardIdAndConfigKey(standard.getId(), configKey);
    }

    /**
     * Whether any workflow type mapping in the standard uses the given org group as its approver.
     */
    @Transactional(readOnly = true)
    public boolean isOrgGroupUsedAsApprover(Standard standard, String orgGroupConfigKey) {
        return templateRepository.existsByStandardIdAndOrgGroupConfigKey(
                standard.getId(), orgGroupConfigKey);
    }

    /**
     * Inserts missing workflow type mapping templates from the system baseline. New templates use
     * the baseline {@code enabled} flag.
     *
     * @return number of newly inserted templates
     */
    @Transactional
    public int ensureMissingFromBaseline(
            Standard standard, List<WorkflowTypeMappingBaseline> baselineItems) {
        if (baselineItems == null || baselineItems.isEmpty()) {
            return 0;
        }
        int added = 0;
        for (WorkflowTypeMappingBaseline item : baselineItems) {
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
            String orgGroupConfigKey =
                    resolveApproverOrgGroupConfigKey(standard, item.orgGroupConfigKey());
            if (orgGroupConfigKey == null) {
                log.warn(
                        "Cannot add workflow type mapping {} to standard {}: no approver org group available",
                        item.configKey(),
                        standard.getId());
                continue;
            }
            templateRepository.save(
                    WorkflowTypeMappingTemplate.builder()
                            .tenantId(standard.getTenantId())
                            .standardId(standard.getId())
                            .configKey(item.configKey())
                            .workflowTypeId(item.workflowTypeId())
                            .orgGroupConfigKey(orgGroupConfigKey)
                            .autoApprove(false)
                            .enabled(item.enabled())
                            .selfApproveAllowed(false)
                            .build());
            added++;
            log.info(
                    "Added workflow type mapping '{}' to standard {} (id={})",
                    item.configKey(),
                    standard.getName(),
                    standard.getId());
        }
        return added;
    }

    private String resolveApproverOrgGroupConfigKey(Standard standard, String preferredKey) {
        if (preferredKey != null
                && orgGroupTemplateRepository.existsByStandardIdAndConfigKey(
                        standard.getId(), preferredKey)) {
            return preferredKey;
        }
        Optional<String> fromExistingMapping =
                templateRepository.findByStandardId(standard.getId()).stream()
                        .map(WorkflowTypeMappingTemplate::getOrgGroupConfigKey)
                        .filter(key -> key != null && !key.isBlank())
                        .filter(
                                key ->
                                        orgGroupTemplateRepository.existsByStandardIdAndConfigKey(
                                                standard.getId(), key))
                        .findFirst();
        if (fromExistingMapping.isPresent()) {
            return fromExistingMapping.get();
        }
        if (preferredKey != null && !preferredKey.isBlank()) {
            return preferredKey;
        }
        return null;
    }

    private Optional<WorkflowTypeMapping> toInstance(
            Long organizationId, WorkflowTypeMappingTemplate template) {
        Optional<OrganizationGroup> approverGroup =
                organizationGroupRepository.findByOrganizationIdAndConfigKey(
                        organizationId, template.getOrgGroupConfigKey());
        if (approverGroup.isEmpty()) {
            log.warn(
                    "Cannot materialize workflow type mapping {} for org {}: approver org group '{}' missing",
                    template.getConfigKey(),
                    organizationId,
                    template.getOrgGroupConfigKey());
            return Optional.empty();
        }
        OrganizationGroup org = approverGroup.get();
        return Optional.of(
                WorkflowTypeMapping.builder()
                        .tenantId(org.getTenantId())
                        .organizationId(organizationId)
                        .workflowTypeId(template.getWorkflowTypeId())
                        .organizationGroupId(org.getId())
                        .autoApprove(template.isAutoApprove())
                        .configKey(template.getConfigKey())
                        .standardOriginId(template.getId())
                        .build());
    }

    private void applyTemplateToInstance(
            Long organizationId,
            WorkflowTypeMapping mapping,
            WorkflowTypeMappingTemplate template) {
        resolveApproverGroupId(organizationId, template).ifPresent(mapping::setOrganizationGroupId);
        mapping.setAutoApprove(template.isAutoApprove());
        mapping.setStandardOriginId(template.getId());
        mappingRepository.save(mapping);
    }

    private void syncDefinitionFlags(Long organizationId, WorkflowTypeMappingTemplate template) {
        definitionRepository
                .findByOrganizationIdAndConfigKey(organizationId, template.getConfigKey())
                .ifPresent(definition -> applyDefinitionFlags(definition, template));
    }

    void applyDefinitionFlags(WorkflowDefinition definition, WorkflowTypeMappingTemplate template) {
        definition.setEnabled(template.isEnabled());
        definition.setSelfApproveAllowed(template.isSelfApproveAllowed());
        definitionRepository.save(definition);
    }

    private Optional<Long> resolveApproverGroupId(
            Long organizationId, WorkflowTypeMappingTemplate template) {
        return organizationGroupRepository
                .findByOrganizationIdAndConfigKey(organizationId, template.getOrgGroupConfigKey())
                .map(OrganizationGroup::getId);
    }
}
