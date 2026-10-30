/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
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
    private final UserOrganizationMembershipRepository membershipRepository;
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
     * Catalog types for the user's membership organization (main, else first active). Empty when
     * the user has no active membership. Used by self-service submission and type-name resolution.
     */
    @Transactional(readOnly = true)
    public List<TrainingCourseTypeDto> listAvailableTypesForUser(Long userId) {
        return resolveMembershipOrganizationId(userId)
                .map(this::getAvailableTypesForOrg)
                .map(OrgTrainingCourseCatalogDto::getTypes)
                .orElse(List.of());
    }

    private Optional<Long> resolveMembershipOrganizationId(Long userId) {
        Instant now = Instant.now();
        return membershipRepository
                .findActiveByUser_IdAndMainTrue(userId, now)
                .or(() -> membershipRepository.findFirstActiveByUser_IdOrderByIdAsc(userId, now))
                .map(membership -> membership.getOrganization().getId());
    }

    @Transactional(readOnly = true)
    public void requireTypeAvailableForOrg(Long tenantTrainingCourseTypeId, Long organizationId) {
        Organization organization = requireOrganization(organizationId);
        TenantTrainingCourseType type =
                tenantTypeRepository
                        .findById(tenantTrainingCourseTypeId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Training course type not found: "
                                                        + tenantTrainingCourseTypeId));
        if (!LifecycleFields.isCatalogSelectable(type)) {
            throw new ConflictException("Training course type is not available");
        }
        Set<Long> catalogIds =
                resolveCatalogTypes(organization).stream()
                        .map(TrainingCourseTypeDto::getId)
                        .collect(Collectors.toSet());
        if (!catalogIds.contains(tenantTrainingCourseTypeId)) {
            throw new ConflictException(
                    "Training course type is not available for organization " + organizationId);
        }
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
