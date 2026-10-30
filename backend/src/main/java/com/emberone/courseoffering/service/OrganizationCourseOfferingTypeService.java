/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import com.emberone.courseoffering.dto.CourseOfferingTypeSettingsDto;
import com.emberone.courseoffering.organization.OrganizationCourseOfferingType;
import com.emberone.courseoffering.organization.OrganizationCourseOfferingTypeRepository;
import com.emberone.module.domain.Module;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.standard.service.EffectiveStandardResolver;
import com.emberone.standard.service.EnabledModuleResolver;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configures which tenant training course types an organization may offer. Only configuration: an
 * offering additionally needs the type in the organization's course catalog (see {@link
 * CourseOfferingCatalogService}). The standard only switches the module on or off.
 */
@Service
@RequiredArgsConstructor
public class OrganizationCourseOfferingTypeService {

    private final OrganizationRepository organizationRepository;
    private final EffectiveStandardResolver effectiveStandardResolver;
    private final EnabledModuleResolver enabledModuleResolver;
    private final OrganizationCourseOfferingTypeRepository typeRepository;
    private final TenantTrainingCourseTypeRepository tenantTypeRepository;

    @Transactional(readOnly = true)
    public CourseOfferingTypeSettingsDto getSettings(Long tenantId, Long organizationId) {
        return toSettings(requireOrganization(tenantId, organizationId));
    }

    @Transactional(readOnly = true)
    public Set<String> selectedConfigKeys(Long organizationId) {
        return typeRepository.findByOrganizationId(organizationId).stream()
                .map(OrganizationCourseOfferingType::getDefinitionConfigKey)
                .collect(Collectors.toSet());
    }

    @Transactional
    public CourseOfferingTypeSettingsDto replace(
            Long tenantId, Long organizationId, List<String> definitionConfigKeys) {
        Organization organization = requireOrganization(tenantId, organizationId);
        if (!isEnabled(organization)) {
            throw new ConflictException("Course offering module is not enabled for organization");
        }
        Set<String> keys = new LinkedHashSet<>();
        for (String configKey : definitionConfigKeys) {
            if (configKey == null || configKey.isBlank()) {
                continue;
            }
            String trimmed = configKey.trim();
            TenantTrainingCourseType type =
                    tenantTypeRepository
                            .findByConfigKey(trimmed)
                            .orElseThrow(
                                    () ->
                                            new ConflictException(
                                                    "Unknown tenant training course type: "
                                                            + trimmed));
            if (type.isArchived()) {
                throw new ConflictException("Tenant training course type is archived: " + trimmed);
            }
            keys.add(trimmed);
        }

        List<OrganizationCourseOfferingType> existing =
                typeRepository.findByOrganizationId(organizationId);
        Set<String> existingKeys =
                existing.stream()
                        .map(OrganizationCourseOfferingType::getDefinitionConfigKey)
                        .collect(Collectors.toSet());
        typeRepository.deleteAll(
                existing.stream()
                        .filter(row -> !keys.contains(row.getDefinitionConfigKey()))
                        .toList());
        typeRepository.saveAll(
                keys.stream()
                        .filter(key -> !existingKeys.contains(key))
                        .map(
                                key ->
                                        OrganizationCourseOfferingType.builder()
                                                .tenantId(tenantId)
                                                .organizationId(organizationId)
                                                .definitionConfigKey(key)
                                                .build())
                        .toList());
        return toSettings(organization);
    }

    boolean isEnabled(Organization organization) {
        return effectiveStandardResolver
                .resolveForOrganization(organization)
                .map(
                        standard ->
                                enabledModuleResolver
                                        .forStandard(standard)
                                        .contains(Module.COURSE_OFFERINGS))
                .orElse(false);
    }

    private CourseOfferingTypeSettingsDto toSettings(Organization organization) {
        boolean enabled = isEnabled(organization);
        List<String> keys =
                enabled
                        ? typeRepository.findByOrganizationId(organization.getId()).stream()
                                .map(OrganizationCourseOfferingType::getDefinitionConfigKey)
                                .sorted()
                                .toList()
                        : List.of();
        return CourseOfferingTypeSettingsDto.builder()
                .enabled(enabled)
                .definitionConfigKeys(keys)
                .build();
    }

    private Organization requireOrganization(Long tenantId, Long organizationId) {
        return organizationRepository
                .findById(organizationId)
                .filter(org -> tenantId.equals(org.getTenantId()))
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }
}
