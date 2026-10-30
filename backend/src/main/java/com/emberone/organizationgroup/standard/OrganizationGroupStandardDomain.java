/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.standard;

import com.emberone.admin.standard.dto.OrgGroupRemovalImpactDto;
import com.emberone.admin.standard.dto.OrgGroupRemovalImpactLevel;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.organizationgroup.event.OrganizationGroupDeletingEvent;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.i18n.SystemGroupNamingService;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.spi.StandardContentItem;
import com.emberone.standard.spi.StandardDomain;
import com.emberone.standard.util.StandardConfigKey;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link StandardDomain} for organization groups. Templates live in {@code
 * std_org_group_templates}; instances are {@code organization_groups} rows carrying {@code
 * config_key} + {@code standard_origin_id}.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class OrganizationGroupStandardDomain implements StandardDomain {

    private final OrgGroupTemplateRepository templateRepository;
    private final OrganizationGroupRepository groupRepository;
    private final OrganizationRepository organizationRepository;
    private final SystemGroupNamingService systemGroupNamingService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserOrganizationGroupMembershipRepository membershipRepository;

    private static String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    @Override
    public String domainKey() {
        return "organization-groups";
    }

    @Override
    @Transactional
    public void copyTemplates(Standard source, Standard target) {
        for (OrgGroupTemplate template : templateRepository.findByStandardId(source.getId())) {
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
        ensureRequiredSystemGroups(standard);
        Organization organization = requireOrganization(organizationId);
        for (OrgGroupTemplate template : templateRepository.findByStandardId(standard.getId())) {
            if (groupRepository
                    .findByOrganizationIdAndConfigKey(organizationId, template.getConfigKey())
                    .isPresent()) {
                continue;
            }
            groupRepository.save(toInstance(organization, template));
        }
    }

    @Override
    @Transactional
    public void reassignForOrg(Long organizationId, Standard standard) {
        ensureRequiredSystemGroups(standard);
        Organization organization = requireOrganization(organizationId);
        Long tenantId = organization.getTenantId();
        List<OrgGroupTemplate> templates = templateRepository.findByStandardId(standard.getId());
        Set<String> templateKeys =
                templates.stream().map(OrgGroupTemplate::getConfigKey).collect(Collectors.toSet());

        for (OrgGroupTemplate template : templates) {
            groupRepository
                    .findByOrganizationIdAndConfigKey(organizationId, template.getConfigKey())
                    .ifPresentOrElse(
                            existing -> applyTemplateToInstance(existing, template, tenantId),
                            () -> groupRepository.save(toInstance(organization, template)));
        }

        for (OrganizationGroup group :
                groupRepository.findByOrganizationIdAndStandardOriginIdIsNotNull(organizationId)) {
            if (group.getConfigKey() == null || templateKeys.contains(group.getConfigKey())) {
                continue;
            }
            removeInstance(group, organization);
        }
    }

    // --- Template editing (custom standards)
    // -------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<StandardContentItem> describeContent(Standard standard) {
        Long tenantId = standard.getTenantId();
        return templateRepository.findByStandardId(standard.getId()).stream()
                .sorted(java.util.Comparator.comparing(OrgGroupTemplate::getConfigKey))
                .map(
                        template ->
                                new StandardContentItem(
                                        domainKey(),
                                        template.getConfigKey(),
                                        resolveName(template, tenantId),
                                        template.getGroupType().name(),
                                        List.of(),
                                        Map.of(
                                                "description",
                                                normalize(template.getDescription()) == null
                                                        ? ""
                                                        : template.getDescription(),
                                                "type",
                                                template.getGroupType().name()),
                                        true))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrgGroupRemovalImpactDto previewRemoval(
            Standard standard, String configKey, List<Organization> orgs) {
        requireTemplate(standard, configKey);
        return buildRemovalImpact(configKey, orgs);
    }

    @Transactional(readOnly = true)
    public List<OrgGroupRemovalImpactDto> removedConfigKeysImpact(
            Standard currentStandard, Standard newStandard, List<Organization> orgs) {
        Set<String> currentKeys =
                templateRepository.findByStandardId(currentStandard.getId()).stream()
                        .map(OrgGroupTemplate::getConfigKey)
                        .collect(Collectors.toSet());
        Set<String> newKeys =
                templateRepository.findByStandardId(newStandard.getId()).stream()
                        .map(OrgGroupTemplate::getConfigKey)
                        .collect(Collectors.toSet());
        currentKeys.removeAll(newKeys);
        return currentKeys.stream().sorted().map(key -> buildRemovalImpact(key, orgs)).toList();
    }

    /**
     * Add a new organization group template and materialize it into the organizations that use the
     * standard. Returns the assigned config key.
     */
    @Transactional
    public String addTemplate(
            Standard standard,
            String name,
            String description,
            OrganizationGroupType type,
            List<Organization> orgs) {
        if (type.isSystem()
                && templateRepository.existsByStandardIdAndGroupType(standard.getId(), type)) {
            throw new ConflictException(
                    "Standard already contains a " + type.name() + " organization group");
        }
        String configKey =
                type == OrganizationGroupType.CUSTOM
                        ? StandardConfigKey.allocateCustomConfigKey(
                                name,
                                key ->
                                        templateRepository.existsByStandardIdAndConfigKey(
                                                standard.getId(), key))
                        : type.name();
        if (type != OrganizationGroupType.CUSTOM
                && templateRepository.existsByStandardIdAndConfigKey(standard.getId(), configKey)) {
            throw new ConflictException(
                    "Organization group '" + configKey + "' already exists in this standard");
        }
        OrgGroupTemplate template =
                templateRepository.save(
                        OrgGroupTemplate.builder()
                                .tenantId(standard.getTenantId())
                                .standardId(standard.getId())
                                .configKey(configKey)
                                .name(type.isSystem() ? normalize(name) : name)
                                .description(normalize(description))
                                .groupType(type)
                                .build());
        for (Organization org : orgs) {
            if (groupRepository
                    .findByOrganizationIdAndConfigKey(org.getId(), configKey)
                    .isEmpty()) {
                groupRepository.save(toInstance(org, template));
            }
        }
        return configKey;
    }

    /**
     * Update name/description of an organization group template (its type and config key stay
     * fixed) and propagate the change to all materialized instances.
     */
    @Transactional
    public void updateTemplate(
            Standard standard,
            String configKey,
            String name,
            String description,
            List<Organization> orgs) {
        OrgGroupTemplate template = requireTemplate(standard, configKey);
        template.setName(name);
        template.setDescription(normalize(description));
        templateRepository.save(template);
        for (Organization org : orgs) {
            groupRepository
                    .findByOrganizationIdAndConfigKey(org.getId(), configKey)
                    .ifPresent(
                            existing ->
                                    applyTemplateToInstance(existing, template, org.getTenantId()));
        }
        log.info(
                "Updated organization group template '{}' in standard {}",
                configKey,
                standard.getId());
    }

    /**
     * Delete an organization group template and propagate removal to organizations using
     * membership-aware archive or hard-delete rules.
     */
    @Transactional
    public void deleteTemplate(Standard standard, String configKey, List<Organization> orgs) {
        OrgGroupTemplate template = requireTemplate(standard, configKey);
        if (template.getGroupType().isSystem()) {
            throw new ConflictException(
                    "Cannot remove required organization group " + template.getGroupType().name());
        }
        templateRepository.delete(template);
        for (Organization org : orgs) {
            groupRepository
                    .findByOrganizationIdAndConfigKey(org.getId(), configKey)
                    .ifPresent(instance -> removeInstance(instance, org));
        }
        log.info(
                "Deleted organization group template '{}' from standard {}",
                configKey,
                standard.getId());
    }

    /**
     * Every standard must define exactly one leadership and one all-members group. Repairs missing
     * templates idempotently before materialization.
     */
    @Transactional
    public void ensureRequiredSystemGroups(Standard standard) {
        for (OrganizationGroupType required :
                List.of(OrganizationGroupType.LEADERSHIP, OrganizationGroupType.ALL_MEMBERS)) {
            if (!templateRepository.existsByStandardIdAndGroupType(standard.getId(), required)) {
                templateRepository.save(
                        OrgGroupTemplate.builder()
                                .tenantId(standard.getTenantId())
                                .standardId(standard.getId())
                                .configKey(required.name())
                                .name(null)
                                .description(null)
                                .groupType(required)
                                .build());
                log.info(
                        "Added missing {} organization group template to standard {}",
                        required,
                        standard.getId());
            }
        }
    }

    private OrgGroupTemplate requireTemplate(Standard standard, String configKey) {
        return templateRepository
                .findByStandardIdAndConfigKey(standard.getId(), configKey)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Organization group '"
                                                + configKey
                                                + "' not found in standard "
                                                + standard.getId()));
    }

    /**
     * Ensures the organization group may be used as a workflow approver (leadership or custom
     * only).
     */
    @Transactional(readOnly = true)
    public void requireWorkflowApproverOrgGroup(Standard standard, String configKey) {
        OrgGroupTemplate template = requireTemplate(standard, configKey);
        if (!template.getGroupType().canApproveWorkflows()) {
            throw new ConflictException(
                    "Organization group '" + configKey + "' cannot be used as a workflow approver");
        }
    }

    private OrgGroupRemovalImpactDto buildRemovalImpact(String configKey, List<Organization> orgs) {
        Instant now = Instant.now();
        int orgsAffected = 0;
        int totalActive = 0;

        for (Organization org : orgs) {
            Optional<OrganizationGroup> instance =
                    groupRepository.findByOrganizationIdAndConfigKey(org.getId(), configKey);
            if (instance.isEmpty()) {
                continue;
            }
            Long groupId = instance.get().getId();
            boolean hasHistory =
                    membershipRepository.existsByOrganizationGroup_IdIn(List.of(groupId));
            if (!hasHistory) {
                continue;
            }
            int activeMembers =
                    membershipRepository.findActiveUserIdsByGroupIds(List.of(groupId), now).size();
            totalActive += activeMembers;
            orgsAffected++;
        }

        OrgGroupRemovalImpactLevel level;
        if (orgsAffected == 0) {
            level = OrgGroupRemovalImpactLevel.NONE;
        } else if (totalActive > 0) {
            level = OrgGroupRemovalImpactLevel.ACTIVE;
        } else {
            level = OrgGroupRemovalImpactLevel.HISTORICAL;
        }

        return OrgGroupRemovalImpactDto.builder()
                .configKey(configKey)
                .level(level)
                .totalOrgsAffected(orgsAffected)
                .totalActiveMemberships(totalActive)
                .build();
    }

    private void removeInstance(OrganizationGroup instance, Organization org) {
        Long groupId = instance.getId();
        boolean hasHistory = membershipRepository.existsByOrganizationGroup_IdIn(List.of(groupId));
        if (!hasHistory) {
            eventPublisher.publishEvent(
                    new OrganizationGroupDeletingEvent(groupId, instance.getTenantId()));
            groupRepository.delete(instance);
            return;
        }
        Instant now = Instant.now();
        membershipRepository.endActiveMembershipsByGroupIds(List.of(groupId), now);
        archive(instance);
    }

    private void archive(OrganizationGroup group) {
        eventPublisher.publishEvent(
                new OrganizationGroupDeletingEvent(group.getId(), group.getTenantId()));
        LifecycleFields.archive(group, Instant.now());
        group.setStandardOriginId(null);
        group.setConfigKey(null);
        groupRepository.save(group);
    }

    private OrganizationGroup toInstance(Organization organization, OrgGroupTemplate template) {
        return OrganizationGroup.builder()
                .organization(organization)
                .tenantId(organization.getTenantId())
                .name(resolveName(template, organization.getTenantId()))
                .description(template.getDescription())
                .type(template.getGroupType())
                .configKey(template.getConfigKey())
                .standardOriginId(template.getId())
                .archived(false)
                .build();
    }

    /** Copy a template's managed fields onto an instance row and re-anchor it to that template. */
    private void applyTemplateToInstance(
            OrganizationGroup group, OrgGroupTemplate template, Long tenantId) {
        group.setName(resolveName(template, tenantId));
        group.setDescription(template.getDescription());
        group.setType(template.getGroupType());
        group.setStandardOriginId(template.getId());
        groupRepository.save(group);
    }

    private String resolveName(OrgGroupTemplate template, Long tenantId) {
        if (template.getName() != null && !template.getName().isBlank()) {
            return template.getName();
        }
        return systemGroupNamingService.resolveLocalizedName(template.getGroupType(), tenantId);
    }

    private Organization requireOrganization(Long organizationId) {
        return organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }
}
