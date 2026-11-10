/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.standard;

import com.emberone.federalstate.baseline.WorkflowTypeMappingBaseline;
import com.emberone.organization.domain.Organization;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.organizationgroup.standard.OrgGroupTemplateRepository;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.exception.ValidationException;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.spi.StandardContentItem;
import com.emberone.standard.spi.StandardDomain;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.domain.WorkflowTypeMapping;
import com.emberone.workflow.domain.WorkflowTypeMappingSlot;
import com.emberone.workflow.event.WorkflowConfigurationChangedEvent;
import com.emberone.workflow.repository.WorkflowDefinitionRepository;
import com.emberone.workflow.repository.WorkflowTypeMappingRepository;
import com.emberone.workflow.spi.WorkflowApproverSlotRoles;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link StandardDomain} for workflow type mappings (which org group approves a workflow type, per
 * approver slot role). Templates: {@code std_workflow_type_mapping_templates} (+ {@code _slots});
 * instances: {@code wf_type_mappings} (+ {@code wf_type_mapping_slots}). Runs after the
 * organization-groups domain (it resolves the approver group per org).
 *
 * <p>Every template and mapping has one slot row per role of its workflow type ({@link
 * WorkflowApproverSlotRoles#rolesOf}). A role that is new for a type (handler gained a role) is
 * added by {@link #ensureMissingFromBaseline} and {@link #materializeForOrg} with the values of the
 * {@code CONTEXT} row.
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
    private final WorkflowHandlerRegistry handlerRegistry;
    private final ApplicationEventPublisher eventPublisher;

    /** Settings of one slot role as edited in the standard. */
    public record SlotSettings(
            String slotRole,
            String orgGroupConfigKey,
            boolean autoApprove,
            boolean selfApproveAllowed) {}

    @Override
    public String domainKey() {
        return "workflow-type-mappings";
    }

    /** The roles of a workflow type in display order (handler roles, then {@code ADDITIONAL}). */
    public List<String> rolesOf(Long workflowTypeId) {
        return WorkflowApproverSlotRoles.rolesOf(handlerRegistry.require(workflowTypeId));
    }

    @Override
    @Transactional
    public void copyTemplates(Standard source, Standard target) {
        for (WorkflowTypeMappingTemplate template :
                templateRepository.findByStandardId(source.getId())) {
            WorkflowTypeMappingTemplate copy =
                    template.toBuilder()
                            .id(null)
                            .tenantId(target.getTenantId())
                            .standardId(target.getId())
                            .slots(new ArrayList<>())
                            .build();
            for (WorkflowTypeMappingTemplateSlot slot : template.getSlots()) {
                copy.getSlots()
                        .add(
                                WorkflowTypeMappingTemplateSlot.builder()
                                        .tenantId(target.getTenantId())
                                        .template(copy)
                                        .slotRole(slot.getSlotRole())
                                        .orgGroupConfigKey(slot.getOrgGroupConfigKey())
                                        .autoApprove(slot.isAutoApprove())
                                        .selfApproveAllowed(slot.isSelfApproveAllowed())
                                        .build());
            }
            templateRepository.save(copy);
        }
    }

    @Override
    @Transactional
    public void materializeForOrg(Long organizationId, Standard standard) {
        for (WorkflowTypeMappingTemplate template :
                templateRepository.findByStandardId(standard.getId())) {
            Optional<WorkflowTypeMapping> existing =
                    mappingRepository.findByOrganizationIdAndConfigKey(
                            organizationId, template.getConfigKey());
            if (existing.isPresent()) {
                addMissingSlots(organizationId, existing.get(), template);
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
                        Comparator.comparing((WorkflowTypeMappingTemplate template) ->
                                        WorkflowType.fromId(template.getWorkflowTypeId())
                                                .openScope())
                                .thenComparingLong(WorkflowTypeMappingTemplate::getWorkflowTypeId))
                .map(template -> {
                    WorkflowType workflowType = WorkflowType.fromId(template.getWorkflowTypeId());
                    Optional<WorkflowTypeMappingTemplateSlot> contextSlot =
                            template.findSlot(WorkflowApproverSlotRoles.CONTEXT);
                    String contextGroupKey =
                            contextSlot
                                    .map(WorkflowTypeMappingTemplateSlot::getOrgGroupConfigKey)
                                    .orElse(null);
                    // The list shows the CONTEXT role; all roles are edited via the detail view.
                    Map<String, String> attributes = new java.util.HashMap<>();
                    attributes.put("openScope", workflowType.openScope().name());
                    attributes.put("approverOrgGroupConfigKey", contextGroupKey);
                    attributes.put(
                            "autoApprove",
                            String.valueOf(
                                    contextSlot
                                            .map(WorkflowTypeMappingTemplateSlot::isAutoApprove)
                                            .orElse(false)));
                    attributes.put("enabled", String.valueOf(template.isEnabled()));
                    attributes.put(
                            "selfApproveAllowed",
                            String.valueOf(
                                    contextSlot
                                            .map(
                                                    WorkflowTypeMappingTemplateSlot
                                                            ::isSelfApproveAllowed)
                                            .orElse(false)));
                    return new StandardContentItem(
                            domainKey(),
                            template.getConfigKey(),
                            workflowType.name(),
                            contextGroupKey,
                            List.of(),
                            attributes,
                            true);
                })
                .toList();
    }

    /**
     * Update the enabled flag and the settings of every slot role of a mapping template and
     * propagate to materialized rows. {@code slots} must contain each role of the workflow type
     * exactly once.
     *
     * @throws ValidationException if a role is unknown, missing or given twice
     */
    @Transactional
    public void updateMappingTemplate(
            Standard standard,
            String configKey,
            boolean enabled,
            List<SlotSettings> slots,
            List<Organization> orgs) {
        WorkflowTypeMappingTemplate template =
                templateRepository
                        .findByStandardIdAndConfigKey(standard.getId(), configKey)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Workflow mapping '"
                                                + configKey
                                                + "' not found in standard "
                                                + standard.getId()));
        requireAllRolesOnce(template.getWorkflowTypeId(), slots);
        template.setEnabled(enabled);
        for (SlotSettings settings : slots) {
            WorkflowTypeMappingTemplateSlot slot =
                    template.findSlot(settings.slotRole())
                            .orElseGet(() -> addTemplateSlot(template, settings.slotRole()));
            slot.setOrgGroupConfigKey(settings.orgGroupConfigKey());
            slot.setAutoApprove(settings.autoApprove());
            slot.setSelfApproveAllowed(settings.selfApproveAllowed());
        }
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

    private void requireAllRolesOnce(Long workflowTypeId, List<SlotSettings> slots) {
        List<String> expected = rolesOf(workflowTypeId);
        Set<String> seen = new HashSet<>();
        for (SlotSettings settings : slots) {
            if (!expected.contains(settings.slotRole())) {
                throw new ValidationException(
                        "Unknown approver slot role '"
                                + settings.slotRole()
                                + "' for workflow type id "
                                + workflowTypeId);
            }
            if (!seen.add(settings.slotRole())) {
                throw new ValidationException(
                        "Approver slot role '" + settings.slotRole() + "' given more than once");
            }
        }
        for (String role : expected) {
            if (!seen.contains(role)) {
                throw new ValidationException("Approver slot role '" + role + "' is required");
            }
        }
    }

    private WorkflowTypeMappingTemplateSlot addTemplateSlot(
            WorkflowTypeMappingTemplate template, String slotRole) {
        WorkflowTypeMappingTemplateSlot slot =
                WorkflowTypeMappingTemplateSlot.builder()
                        .tenantId(template.getTenantId())
                        .template(template)
                        .slotRole(slotRole)
                        .orgGroupConfigKey("")
                        .build();
        template.getSlots().add(slot);
        return slot;
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
     * Whether any slot of any workflow type mapping in the standard uses the given org group as its
     * approver.
     */
    @Transactional(readOnly = true)
    public boolean isOrgGroupUsedAsApprover(Standard standard, String orgGroupConfigKey) {
        return templateRepository.existsByStandardIdAndSlotOrgGroupConfigKey(
                standard.getId(), orgGroupConfigKey);
    }

    /**
     * Inserts missing workflow type mapping templates from the system baseline and adds missing
     * slot roles to existing templates. New templates use the baseline {@code enabled} flag and one
     * slot per role; a role that is new on an existing template copies the {@code CONTEXT} row.
     *
     * @return number of newly inserted templates and slot rows
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
            Optional<WorkflowTypeMappingTemplate> existing =
                    templateRepository.findByStandardIdAndConfigKey(
                            standard.getId(), item.configKey());
            if (existing.isPresent()) {
                added += addMissingTemplateSlots(standard, existing.get());
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
            WorkflowTypeMappingTemplate template =
                    WorkflowTypeMappingTemplate.builder()
                            .tenantId(standard.getTenantId())
                            .standardId(standard.getId())
                            .configKey(item.configKey())
                            .workflowTypeId(item.workflowTypeId())
                            .enabled(item.enabled())
                            .build();
            for (String role : rolesOf(item.workflowTypeId())) {
                template.getSlots()
                        .add(
                                WorkflowTypeMappingTemplateSlot.builder()
                                        .tenantId(standard.getTenantId())
                                        .template(template)
                                        .slotRole(role)
                                        .orgGroupConfigKey(orgGroupConfigKey)
                                        .autoApprove(false)
                                        .selfApproveAllowed(false)
                                        .build());
            }
            templateRepository.save(template);
            added += 1 + template.getSlots().size();
            log.info(
                    "Added workflow type mapping '{}' to standard {} (id={})",
                    item.configKey(),
                    standard.getName(),
                    standard.getId());
        }
        return added;
    }

    /** Adds a row for each role of the type the template has none for, copying {@code CONTEXT}. */
    private int addMissingTemplateSlots(Standard standard, WorkflowTypeMappingTemplate template) {
        Optional<WorkflowTypeMappingTemplateSlot> contextSlot =
                template.findSlot(WorkflowApproverSlotRoles.CONTEXT);
        if (contextSlot.isEmpty()) {
            log.warn(
                    "Workflow type mapping {} in standard {} has no CONTEXT slot",
                    template.getConfigKey(),
                    standard.getId());
            return 0;
        }
        int added = 0;
        for (String role : rolesOf(template.getWorkflowTypeId())) {
            if (template.findSlot(role).isPresent()) {
                continue;
            }
            template.getSlots()
                    .add(
                            WorkflowTypeMappingTemplateSlot.builder()
                                    .tenantId(template.getTenantId())
                                    .template(template)
                                    .slotRole(role)
                                    .orgGroupConfigKey(contextSlot.get().getOrgGroupConfigKey())
                                    .autoApprove(contextSlot.get().isAutoApprove())
                                    .selfApproveAllowed(contextSlot.get().isSelfApproveAllowed())
                                    .build());
            added++;
            log.info(
                    "Added slot role '{}' to workflow type mapping '{}' in standard {}",
                    role,
                    template.getConfigKey(),
                    standard.getId());
        }
        if (added > 0) {
            templateRepository.save(template);
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
                        .flatMap(template -> template.getSlots().stream())
                        .map(WorkflowTypeMappingTemplateSlot::getOrgGroupConfigKey)
                        .filter(key -> key != null && !key.isBlank())
                        .filter(key ->
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

    /** Creates the mapping with a slot per template slot; empty if an approver group is missing. */
    private Optional<WorkflowTypeMapping> toInstance(
            Long organizationId, WorkflowTypeMappingTemplate template) {
        WorkflowTypeMapping mapping =
                WorkflowTypeMapping.builder()
                        .tenantId(template.getTenantId())
                        .organizationId(organizationId)
                        .workflowTypeId(template.getWorkflowTypeId())
                        .configKey(template.getConfigKey())
                        .standardOriginId(template.getId())
                        .build();
        for (WorkflowTypeMappingTemplateSlot templateSlot : template.getSlots()) {
            Optional<Long> groupId = resolveApproverGroupId(organizationId, templateSlot);
            if (groupId.isEmpty()) {
                log.warn(
                        "Cannot materialize workflow type mapping {} for org {}: approver org group '{}' missing",
                        template.getConfigKey(),
                        organizationId,
                        templateSlot.getOrgGroupConfigKey());
                return Optional.empty();
            }
            mapping.getSlots().add(newMappingSlot(mapping, templateSlot, groupId.get()));
        }
        return Optional.of(mapping);
    }

    private static WorkflowTypeMappingSlot newMappingSlot(
            WorkflowTypeMapping mapping,
            WorkflowTypeMappingTemplateSlot templateSlot,
            Long organizationGroupId) {
        return WorkflowTypeMappingSlot.builder()
                .tenantId(mapping.getTenantId())
                .mapping(mapping)
                .slotRole(templateSlot.getSlotRole())
                .organizationGroupId(organizationGroupId)
                .autoApprove(templateSlot.isAutoApprove())
                .selfApproveAllowed(templateSlot.isSelfApproveAllowed())
                .build();
    }

    /** Adds the slots the mapping lacks (new roles after a baseline sync). */
    private void addMissingSlots(
            Long organizationId,
            WorkflowTypeMapping mapping,
            WorkflowTypeMappingTemplate template) {
        boolean changed = false;
        for (WorkflowTypeMappingTemplateSlot templateSlot : template.getSlots()) {
            if (mapping.findSlot(templateSlot.getSlotRole()).isPresent()) {
                continue;
            }
            Optional<Long> groupId = resolveApproverGroupId(organizationId, templateSlot);
            if (groupId.isEmpty()) {
                log.warn(
                        "Cannot add slot role {} of workflow type mapping {} for org {}: approver org group '{}' missing",
                        templateSlot.getSlotRole(),
                        template.getConfigKey(),
                        organizationId,
                        templateSlot.getOrgGroupConfigKey());
                continue;
            }
            mapping.getSlots().add(newMappingSlot(mapping, templateSlot, groupId.get()));
            changed = true;
        }
        if (changed) {
            mappingRepository.save(mapping);
        }
    }

    private void applyTemplateToInstance(
            Long organizationId,
            WorkflowTypeMapping mapping,
            WorkflowTypeMappingTemplate template) {
        Set<String> templateRoles = new HashSet<>();
        for (WorkflowTypeMappingTemplateSlot templateSlot : template.getSlots()) {
            templateRoles.add(templateSlot.getSlotRole());
            Optional<Long> groupId = resolveApproverGroupId(organizationId, templateSlot);
            Optional<WorkflowTypeMappingSlot> existing =
                    mapping.findSlot(templateSlot.getSlotRole());
            if (existing.isPresent()) {
                groupId.ifPresent(existing.get()::setOrganizationGroupId);
                existing.get().setAutoApprove(templateSlot.isAutoApprove());
                existing.get().setSelfApproveAllowed(templateSlot.isSelfApproveAllowed());
            } else if (groupId.isPresent()) {
                mapping.getSlots().add(newMappingSlot(mapping, templateSlot, groupId.get()));
            }
        }
        mapping.getSlots().removeIf(slot -> !templateRoles.contains(slot.getSlotRole()));
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
        definitionRepository.save(definition);
    }

    private Optional<Long> resolveApproverGroupId(
            Long organizationId, WorkflowTypeMappingTemplateSlot templateSlot) {
        return organizationGroupRepository
                .findByOrganizationIdAndConfigKey(
                        organizationId, templateSlot.getOrgGroupConfigKey())
                .map(OrganizationGroup::getId);
    }
}
