/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permissiongroup.standard;

import com.emberone.module.domain.Module;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.permission.domain.PermissionGroupType;
import com.emberone.permission.domain.PermissionType;
import com.emberone.permission.event.PermissionGroupPermissionsChangedEvent;
import com.emberone.permissiongroup.domain.PermissionGroup;
import com.emberone.permissiongroup.repository.PermissionGroupRepository;
import com.emberone.permissiongroupmapping.repository.PermissionGroupMappingRepository;
import com.emberone.permissiongroupmapping.service.PermissionGroupMappingService;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.i18n.PermissionGroupNamingService;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.service.EnabledModuleResolver;
import com.emberone.standard.spi.StandardContentItem;
import com.emberone.standard.spi.StandardDomain;
import com.emberone.standard.util.StandardConfigKey;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link StandardDomain} for permission groups. Templates live in {@code
 * std_permission_group_templates} (with {@code std_permission_group_template_grants}) plus
 * org-group&rarr;permission-group mapping templates ({@code
 * std_org_group_permission_group_templates}). Instances are {@code perm_groups} rows (+ their
 * grants) and {@code perm_group_mappings} rows.
 *
 * <p>Runs after the organization-groups domain (it materializes mappings that reference the org's
 * groups).
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class PermissionGroupStandardDomain implements StandardDomain {

    /**
     * Synthetic content section for organization-group &rarr; permission-group mapping templates.
     */
    public static final String MAPPINGS_SECTION = "group-mappings";

    private final PermissionGroupTemplateRepository templateRepository;
    private final OrgGroupPermissionGroupTemplateRepository mappingTemplateRepository;
    private final PermissionGroupRepository permissionGroupRepository;
    private final OrganizationGroupRepository organizationGroupRepository;
    private final PermissionGroupMappingService permissionGroupMappingService;
    private final PermissionGroupMappingRepository permissionGroupMappingRepository;
    private final OrganizationRepository organizationRepository;
    private final PermissionGroupNamingService permissionGroupNamingService;
    private final ApplicationEventPublisher eventPublisher;
    private final EnabledModuleResolver enabledModuleResolver;

    private static String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    @Override
    public String domainKey() {
        return "permission-groups";
    }

    @Override
    @Transactional
    public void copyTemplates(Standard source, Standard target) {
        for (PermissionGroupTemplate template :
                templateRepository.findByStandardId(source.getId())) {
            PermissionGroupTemplate copy =
                    PermissionGroupTemplate.builder()
                            .tenantId(target.getTenantId())
                            .standardId(target.getId())
                            .configKey(template.getConfigKey())
                            .name(template.getName())
                            .description(template.getDescription())
                            .groupType(template.getGroupType())
                            .grants(new ArrayList<>())
                            .build();
            copy.setPermissionTypes(
                    template.getGroupType()
                            .effectivePermissionTypes(template.getPermissionTypes()));
            templateRepository.save(copy);
        }
        for (OrgGroupPermissionGroupTemplate mapping :
                mappingTemplateRepository.findByStandardId(source.getId())) {
            mappingTemplateRepository.save(
                    mapping.toBuilder()
                            .id(null)
                            .tenantId(target.getTenantId())
                            .standardId(target.getId())
                            .build());
        }
    }

    @Override
    @Transactional
    public void materializeForOrg(Long organizationId, Standard standard) {
        Organization organization = requireOrganization(organizationId);
        for (PermissionGroupTemplate template :
                templateRepository.findByStandardId(standard.getId())) {
            if (permissionGroupRepository
                    .findByOrganizationIdAndConfigKey(organizationId, template.getConfigKey())
                    .isPresent()) {
                continue;
            }
            permissionGroupRepository.save(toInstance(organization, template, standard));
        }
        materializeMappings(organizationId, standard);
    }

    @Override
    @Transactional
    public void reassignForOrg(Long organizationId, Standard standard) {
        Organization organization = requireOrganization(organizationId);
        List<PermissionGroupTemplate> templates =
                templateRepository.findByStandardId(standard.getId());
        Set<String> templateKeys =
                templates.stream()
                        .map(PermissionGroupTemplate::getConfigKey)
                        .collect(Collectors.toSet());

        for (PermissionGroupTemplate template : templates) {
            permissionGroupRepository
                    .findByOrganizationIdAndConfigKey(organizationId, template.getConfigKey())
                    .ifPresentOrElse(
                            existing ->
                                    applyTemplateToInstance(
                                            existing,
                                            template,
                                            organization.getTenantId(),
                                            standard),
                            () ->
                                    permissionGroupRepository.save(
                                            toInstance(organization, template, standard)));
        }

        for (PermissionGroup group :
                permissionGroupRepository.findByOrganizationIdAndStandardOriginIdIsNotNull(
                        organizationId)) {
            if (group.getConfigKey() == null || templateKeys.contains(group.getConfigKey())) {
                continue;
            }
            permissionGroupMappingService.deleteMappingsByPermissionGroupId(group.getId());
            permissionGroupRepository.delete(group);
        }

        removeStaleMappings(organizationId, standard);
        materializeMappings(organizationId, standard);
    }

    // --- Template editing (custom standards)
    // -------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<StandardContentItem> describeContent(Standard standard) {
        Long tenantId = standard.getTenantId();
        List<StandardContentItem> items = new ArrayList<>();
        templateRepository.findByStandardId(standard.getId()).stream()
                .sorted(java.util.Comparator.comparing(PermissionGroupTemplate::getConfigKey))
                .forEach(
                        template ->
                                items.add(
                                        new StandardContentItem(
                                                domainKey(),
                                                template.getConfigKey(),
                                                resolveName(template, tenantId),
                                                template.getGroupType().name(),
                                                template
                                                        .getGroupType()
                                                        .effectivePermissionTypes(
                                                                template.getPermissionTypes())
                                                        .stream()
                                                        .map(PermissionType::name)
                                                        .sorted()
                                                        .toList(),
                                                Map.of(
                                                        "description",
                                                        normalize(template.getDescription()) == null
                                                                ? ""
                                                                : template.getDescription(),
                                                        "type",
                                                        template.getGroupType().name()),
                                                true)));
        mappingTemplateRepository.findByStandardId(standard.getId()).stream()
                .sorted(
                        java.util.Comparator.comparing(
                                        OrgGroupPermissionGroupTemplate::getOrgGroupConfigKey)
                                .thenComparing(
                                        OrgGroupPermissionGroupTemplate
                                                ::getPermissionGroupConfigKey))
                .forEach(
                        mapping ->
                                items.add(
                                        new StandardContentItem(
                                                MAPPINGS_SECTION,
                                                mapping.getOrgGroupConfigKey()
                                                        + "::"
                                                        + mapping.getPermissionGroupConfigKey(),
                                                mapping.getOrgGroupConfigKey(),
                                                mapping.getPermissionGroupConfigKey(),
                                                List.of(),
                                                Map.of(
                                                        "orgGroupConfigKey",
                                                                mapping.getOrgGroupConfigKey(),
                                                        "permissionGroupConfigKey",
                                                                mapping
                                                                        .getPermissionGroupConfigKey()),
                                                true)));
        return items;
    }

    /**
     * Add a new permission group template with its granted permission types and materialize it into
     * the organizations that use the standard. Returns the config key.
     */
    @Transactional
    public String addTemplate(
            Standard standard,
            String name,
            String description,
            PermissionGroupType type,
            List<PermissionType> permissionTypes,
            List<Organization> orgs) {
        if (type != PermissionGroupType.CUSTOM) {
            throw new ConflictException("Only custom permission groups can be added to a standard");
        }
        String configKey =
                StandardConfigKey.allocateCustomConfigKey(
                        name,
                        key ->
                                templateRepository.existsByStandardIdAndConfigKey(
                                        standard.getId(), key));
        PermissionGroupTemplate template =
                PermissionGroupTemplate.builder()
                        .tenantId(standard.getTenantId())
                        .standardId(standard.getId())
                        .configKey(configKey)
                        .name(name)
                        .description(normalize(description))
                        .groupType(type)
                        .grants(new ArrayList<>())
                        .build();
        template.setPermissionTypes(type.effectivePermissionTypes(permissionTypes));
        templateRepository.save(template);
        for (Organization org : orgs) {
            if (permissionGroupRepository
                    .findByOrganizationIdAndConfigKey(org.getId(), configKey)
                    .isEmpty()) {
                permissionGroupRepository.save(toInstance(org, template, standard));
            }
        }
        log.info(
                "Added permission group '{}' to standard '{}' (id={})",
                configKey,
                standard.getName(),
                standard.getId());
        return configKey;
    }

    /**
     * Update name/description and granted permission types of a permission group template,
     * propagating the change to organizations whose instance still matched the pre-edit values.
     */
    @Transactional
    public void updateTemplate(
            Standard standard,
            String configKey,
            String name,
            String description,
            List<PermissionType> permissionTypes,
            List<Organization> orgs) {
        PermissionGroupTemplate template = requireTemplate(standard, configKey);
        template.setName(name);
        template.setDescription(normalize(description));
        template.setPermissionTypes(
                template.getGroupType().effectivePermissionTypes(permissionTypes));
        templateRepository.save(template);
        for (Organization org : orgs) {
            permissionGroupRepository
                    .findByOrganizationIdAndConfigKey(org.getId(), configKey)
                    .ifPresent(
                            existing -> {
                                applyTemplateToInstance(
                                        existing, template, org.getTenantId(), standard);
                                eventPublisher.publishEvent(
                                        new PermissionGroupPermissionsChangedEvent(
                                                existing.getTenantId(), existing.getId()));
                            });
        }
        log.info(
                "Updated permission group '{}' in standard '{}' (id={})",
                configKey,
                standard.getName(),
                standard.getId());
    }

    /**
     * Re-materialize permission group instances after the standard's enabled {@link Module} set
     * changed: strips disabled-module grants from custom templates and from every org instance
     * (including ADMIN).
     */
    @Transactional
    public void syncInstancesForEnabledModules(
            Standard standard, Set<Module> enabledModules, List<Organization> orgs) {
        for (PermissionGroupTemplate template :
                templateRepository.findByStandardId(standard.getId())) {
            if (template.getGroupType() == PermissionGroupType.CUSTOM) {
                List<PermissionType> filteredTemplateGrants =
                        Module.filterPermissions(template.getPermissionTypes(), enabledModules);
                template.setPermissionTypes(filteredTemplateGrants);
                templateRepository.save(template);
            }
            for (Organization org : orgs) {
                permissionGroupRepository
                        .findByOrganizationIdAndConfigKey(org.getId(), template.getConfigKey())
                        .ifPresent(
                                existing -> {
                                    applyTemplateToInstance(
                                            existing, template, org.getTenantId(), standard);
                                    eventPublisher.publishEvent(
                                            new PermissionGroupPermissionsChangedEvent(
                                                    existing.getTenantId(), existing.getId()));
                                });
            }
        }
        log.info(
                "Synced permission groups for enabled modules on standard '{}' (id={})",
                standard.getName(),
                standard.getId());
    }

    /**
     * Delete a permission group template and any mapping templates referencing it, then hard-delete
     * the corresponding instance rows (and their mappings) in every affected organization.
     */
    @Transactional
    public void deleteTemplate(Standard standard, String configKey, List<Organization> orgs) {
        PermissionGroupTemplate template = requireTemplate(standard, configKey);
        if (template.getGroupType().isSystem()) {
            throw new ConflictException(
                    "Cannot remove required permission group " + template.getGroupType().name());
        }
        mappingTemplateRepository.deleteAll(
                mappingTemplateRepository.findByStandardIdAndPermissionGroupConfigKey(
                        standard.getId(), configKey));
        templateRepository.delete(template);
        for (Organization org : orgs) {
            permissionGroupRepository
                    .findByOrganizationIdAndConfigKey(org.getId(), configKey)
                    .ifPresent(
                            existing -> {
                                permissionGroupMappingService.deleteMappingsByPermissionGroupId(
                                        existing.getId());
                                permissionGroupRepository.delete(existing);
                            });
        }
        log.info(
                "Removed permission group '{}' from standard '{}' (id={})",
                configKey,
                standard.getName(),
                standard.getId());
    }

    /**
     * Map an organization group to a permission group within the standard and materialize the
     * mapping.
     */
    @Transactional
    public void addMapping(
            Standard standard,
            String orgGroupConfigKey,
            String permissionGroupConfigKey,
            List<Organization> orgs) {
        if (mappingTemplateRepository
                .existsByStandardIdAndOrgGroupConfigKeyAndPermissionGroupConfigKey(
                        standard.getId(), orgGroupConfigKey, permissionGroupConfigKey)) {
            throw new ConflictException("Mapping already exists in this standard");
        }
        mappingTemplateRepository.save(
                OrgGroupPermissionGroupTemplate.builder()
                        .tenantId(standard.getTenantId())
                        .standardId(standard.getId())
                        .orgGroupConfigKey(orgGroupConfigKey)
                        .permissionGroupConfigKey(permissionGroupConfigKey)
                        .build());
        for (Organization org : orgs) {
            materializeMapping(org.getId(), orgGroupConfigKey, permissionGroupConfigKey);
        }
        log.info(
                "Added permission group mapping {} -> {} to standard '{}' (id={})",
                orgGroupConfigKey,
                permissionGroupConfigKey,
                standard.getName(),
                standard.getId());
    }

    /** Remove a mapping template and the corresponding standard-managed instance mappings. */
    @Transactional
    public void deleteMapping(
            Standard standard,
            String orgGroupConfigKey,
            String permissionGroupConfigKey,
            List<Organization> orgs) {
        mappingTemplateRepository
                .findByStandardIdAndOrgGroupConfigKeyAndPermissionGroupConfigKey(
                        standard.getId(), orgGroupConfigKey, permissionGroupConfigKey)
                .ifPresent(mappingTemplateRepository::delete);
        for (Organization org : orgs) {
            Optional<OrganizationGroup> orgGroup =
                    organizationGroupRepository.findByOrganizationIdAndConfigKey(
                            org.getId(), orgGroupConfigKey);
            Optional<PermissionGroup> permissionGroup =
                    permissionGroupRepository.findByOrganizationIdAndConfigKey(
                            org.getId(), permissionGroupConfigKey);
            if (orgGroup.isPresent() && permissionGroup.isPresent()) {
                permissionGroupMappingService.deleteStandardMapping(
                        orgGroup.get().getId(), permissionGroup.get().getId());
            }
        }
        log.info(
                "Removed permission group mapping {} -> {} from standard '{}' (id={})",
                orgGroupConfigKey,
                permissionGroupConfigKey,
                standard.getName(),
                standard.getId());
    }

    /**
     * Remove all mapping templates that reference an organization group (used when that group is
     * deleted).
     */
    @Transactional
    public void deleteMappingsForOrgGroup(Standard standard, String orgGroupConfigKey) {
        mappingTemplateRepository.deleteAll(
                mappingTemplateRepository.findByStandardIdAndOrgGroupConfigKey(
                        standard.getId(), orgGroupConfigKey));
        log.info(
                "Removed permission group mappings for organization group '{}' from standard '{}' (id={})",
                orgGroupConfigKey,
                standard.getName(),
                standard.getId());
    }

    private PermissionGroupTemplate requireTemplate(Standard standard, String configKey) {
        return templateRepository
                .findByStandardIdAndConfigKey(standard.getId(), configKey)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Permission group '"
                                                + configKey
                                                + "' not found in standard "
                                                + standard.getId()));
    }

    private void materializeMappings(Long organizationId, Standard standard) {
        for (OrgGroupPermissionGroupTemplate mapping :
                mappingTemplateRepository.findByStandardId(standard.getId())) {
            materializeMapping(
                    organizationId,
                    mapping.getOrgGroupConfigKey(),
                    mapping.getPermissionGroupConfigKey());
        }
    }

    private void materializeMapping(
            Long organizationId, String orgGroupConfigKey, String permissionGroupConfigKey) {
        Optional<OrganizationGroup> orgGroup =
                organizationGroupRepository.findByOrganizationIdAndConfigKey(
                        organizationId, orgGroupConfigKey);
        Optional<PermissionGroup> permissionGroup =
                permissionGroupRepository.findByOrganizationIdAndConfigKey(
                        organizationId, permissionGroupConfigKey);
        if (orgGroup.isPresent() && permissionGroup.isPresent()) {
            permissionGroupMappingService.createStandardMapping(
                    orgGroup.get().getId(), permissionGroup.get().getId());
        } else {
            log.warn(
                    "Skipping mapping {}->{} for org {}: org group present={}, permission group present={}",
                    orgGroupConfigKey,
                    permissionGroupConfigKey,
                    organizationId,
                    orgGroup.isPresent(),
                    permissionGroup.isPresent());
        }
    }

    /**
     * Remove standard-managed instance mappings that no longer correspond to a mapping template of
     * the standard. Manual mappings are never touched (their {@code standardManaged} flag is
     * false).
     */
    private void removeStaleMappings(Long organizationId, Standard standard) {
        Set<String> desired = new java.util.HashSet<>();
        for (OrgGroupPermissionGroupTemplate template :
                mappingTemplateRepository.findByStandardId(standard.getId())) {
            Optional<OrganizationGroup> orgGroup =
                    organizationGroupRepository.findByOrganizationIdAndConfigKey(
                            organizationId, template.getOrgGroupConfigKey());
            Optional<PermissionGroup> permissionGroup =
                    permissionGroupRepository.findByOrganizationIdAndConfigKey(
                            organizationId, template.getPermissionGroupConfigKey());
            if (orgGroup.isPresent() && permissionGroup.isPresent()) {
                desired.add(orgGroup.get().getId() + ":" + permissionGroup.get().getId());
            }
        }
        for (OrganizationGroup orgGroup :
                organizationGroupRepository.findByOrganizationId(organizationId)) {
            permissionGroupMappingRepository.findByOrganizationGroupId(orgGroup.getId()).stream()
                    .filter(m -> m.isStandardManaged())
                    .filter(
                            m ->
                                    !desired.contains(
                                            orgGroup.getId()
                                                    + ":"
                                                    + m.getPermissionGroup().getId()))
                    .map(m -> m.getPermissionGroup().getId())
                    .toList()
                    .forEach(
                            pgId ->
                                    permissionGroupMappingService.deleteStandardMapping(
                                            orgGroup.getId(), pgId));
        }
    }

    private PermissionGroup toInstance(
            Organization organization, PermissionGroupTemplate template, Standard standard) {
        PermissionGroup group =
                PermissionGroup.builder()
                        .tenantId(organization.getTenantId())
                        .organizationId(organization.getId())
                        .name(resolveName(template, organization.getTenantId()))
                        .description(template.getDescription())
                        .type(template.getGroupType())
                        .configKey(template.getConfigKey())
                        .standardOriginId(template.getId())
                        .permissionTypeLinks(new ArrayList<>())
                        .build();
        group.setPermissionTypes(materializedPermissionTypes(template, standard));
        return group;
    }

    /** Copy a template's managed fields onto an instance row and re-anchor it to that template. */
    private void applyTemplateToInstance(
            PermissionGroup group,
            PermissionGroupTemplate template,
            Long tenantId,
            Standard standard) {
        group.setName(resolveName(template, tenantId));
        group.setDescription(template.getDescription());
        group.setType(template.getGroupType());
        group.setStandardOriginId(template.getId());
        group.setPermissionTypes(materializedPermissionTypes(template, standard));
        permissionGroupRepository.save(group);
    }

    private List<PermissionType> materializedPermissionTypes(
            PermissionGroupTemplate template, Standard standard) {
        Set<Module> enabledModules = enabledModuleResolver.forStandard(standard);
        return Module.filterPermissions(
                template.getGroupType().effectivePermissionTypes(template.getPermissionTypes()),
                enabledModules);
    }

    private String resolveName(PermissionGroupTemplate template, Long tenantId) {
        return resolveName(template.getGroupType(), template.getName(), tenantId);
    }

    private String resolveName(PermissionGroupType type, String name, Long tenantId) {
        if (name != null && !name.isBlank()) {
            return name;
        }
        return permissionGroupNamingService.resolveLocalizedName(type, tenantId);
    }

    private Organization requireOrganization(Long organizationId) {
        return organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }
}
