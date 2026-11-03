/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.trainingcourse.service;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.service.EffectiveStandardResolver;
import com.emberone.trainingcourse.dto.OrgTrainingCourseCatalogDto;
import com.emberone.trainingcourse.dto.TrainingCourseTypeDto;
import com.emberone.trainingcourse.standard.TrainingCourseTypeSelection;
import com.emberone.trainingcourse.standard.TrainingCourseTypeSelectionRepository;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import com.emberone.userorganizationmembership.service.ActiveOrganizationMembershipLookup;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrainingCourseCatalogService {

    private final OrganizationRepository organizationRepository;
    private final ActiveOrganizationMembershipLookup activeOrganizationMembershipLookup;
    private final EffectiveStandardResolver effectiveStandardResolver;
    private final TrainingCourseTypeSelectionRepository selectionRepository;
    private final TenantTrainingCourseTypeRepository tenantTypeRepository;

    static TrainingCourseTypeDto toTypeDto(TenantTrainingCourseType type) {
        return TrainingCourseTypeDto.builder()
                .id(type.getId())
                .name(type.getName())
                .active(type.isActive())
                .configKey(type.getConfigKey())
                .build();
    }

    @Transactional(readOnly = true)
    public OrgTrainingCourseCatalogDto getAvailableTypesForOrg(Long organizationId) {
        Organization organization = requireOrganization(organizationId);
        List<TrainingCourseTypeDto> types =
                resolveCatalogTypes(organization).stream()
                        .sorted(
                                Comparator.comparing(
                                        TrainingCourseTypeDto::getName,
                                        String.CASE_INSENSITIVE_ORDER))
                        .toList();
        return OrgTrainingCourseCatalogDto.builder()
                .organizationId(organizationId)
                .types(types)
                .build();
    }

    /**
     * Catalog types from the union of effective-standard catalogs of every organization where the
     * user has an active membership. Empty when the user has no active membership. Used by
     * self-service submission and type-name resolution.
     */
    @Transactional(readOnly = true)
    public List<TrainingCourseTypeDto> listAvailableTypesForUser(Long userId) {
        Map<Long, TrainingCourseTypeDto> byId = new LinkedHashMap<>();
        for (Long organizationId :
                activeOrganizationMembershipLookup.listActiveOrganizationIds(userId)) {
            for (TrainingCourseTypeDto type : getAvailableTypesForOrg(organizationId).getTypes()) {
                byId.putIfAbsent(type.getId(), type);
            }
        }
        return byId.values().stream()
                .sorted(
                        Comparator.comparing(
                                TrainingCourseTypeDto::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public void requireTypeAvailableForOrg(Long tenantTrainingCourseTypeId, Long organizationId) {
        Organization organization = requireOrganization(organizationId);
        requireTypeSelectable(tenantTrainingCourseTypeId);
        if (!catalogContainsType(organization, tenantTrainingCourseTypeId)) {
            throw new ConflictException(
                    "Training course type is not available for organization " + organizationId);
        }
    }

    /**
     * Ensures the type is selectable in at least one active-membership organization catalog of the
     * user. Used by self-service workflows.
     */
    @Transactional(readOnly = true)
    public void requireTypeAvailableForUser(Long tenantTrainingCourseTypeId, Long userId) {
        requireTypeSelectable(tenantTrainingCourseTypeId);
        for (Long organizationId :
                activeOrganizationMembershipLookup.listActiveOrganizationIds(userId)) {
            Organization organization = requireOrganization(organizationId);
            if (catalogContainsType(organization, tenantTrainingCourseTypeId)) {
                return;
            }
        }
        throw new ConflictException(
                "Training course type is not available for any of the user's organizations");
    }

    private void requireTypeSelectable(Long tenantTrainingCourseTypeId) {
        TenantTrainingCourseType type =
                tenantTypeRepository
                        .findById(tenantTrainingCourseTypeId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Training course type not found: "
                                                + tenantTrainingCourseTypeId));
        if (!LifecycleFields.isCatalogSelectable(type)) {
            throw new ConflictException("Training course type is not available");
        }
    }

    private boolean catalogContainsType(
            Organization organization, Long tenantTrainingCourseTypeId) {
        return resolveCatalogTypes(organization).stream()
                .map(TrainingCourseTypeDto::getId)
                .anyMatch(tenantTrainingCourseTypeId::equals);
    }

    private List<TrainingCourseTypeDto> resolveCatalogTypes(Organization organization) {
        Optional<Standard> standard =
                effectiveStandardResolver.resolveForOrganization(organization);
        if (standard.isEmpty()) {
            return List.of();
        }
        Set<String> selectedKeys =
                selectionRepository.findByStandardId(standard.get().getId()).stream()
                        .map(TrainingCourseTypeSelection::getDefinitionConfigKey)
                        .collect(Collectors.toSet());
        if (selectedKeys.isEmpty()) {
            return List.of();
        }
        return tenantTypeRepository.findByConfigKeyIn(selectedKeys).stream()
                .filter(type -> !type.isArchived())
                .map(TrainingCourseCatalogService::toTypeDto)
                .toList();
    }

    private Organization requireOrganization(Long organizationId) {
        return organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }
}
