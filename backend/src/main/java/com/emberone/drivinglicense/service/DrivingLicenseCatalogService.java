/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.drivinglicense.service;

import com.emberone.drivinglicense.dto.DrivingLicenseDefinitionDto;
import com.emberone.drivinglicense.dto.OrgDrivingLicenseCatalogDto;
import com.emberone.drivinglicense.standard.DrivingLicenseDefinitionSelection;
import com.emberone.drivinglicense.standard.DrivingLicenseDefinitionSelectionRepository;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinition;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinitionRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.service.EffectiveStandardResolver;
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
public class DrivingLicenseCatalogService {

    private final OrganizationRepository organizationRepository;
    private final UserOrganizationMembershipRepository membershipRepository;
    private final EffectiveStandardResolver effectiveStandardResolver;
    private final DrivingLicenseDefinitionSelectionRepository selectionRepository;
    private final TenantDrivingLicenseDefinitionRepository tenantDefinitionRepository;

    static DrivingLicenseDefinitionDto toDefinitionDto(TenantDrivingLicenseDefinition definition) {
        return DrivingLicenseDefinitionDto.builder()
                .id(definition.getId())
                .name(definition.getName())
                .active(definition.isActive())
                .configKey(definition.getConfigKey())
                .build();
    }

    @Transactional(readOnly = true)
    public OrgDrivingLicenseCatalogDto getAvailableDefinitionsForOrg(Long organizationId) {
        Organization organization = requireOrganization(organizationId);
        List<DrivingLicenseDefinitionDto> definitions =
                resolveCatalogDefinitions(organization).stream()
                        .sorted(
                                Comparator.comparing(
                                        DrivingLicenseDefinitionDto::getName,
                                        String.CASE_INSENSITIVE_ORDER))
                        .toList();
        return OrgDrivingLicenseCatalogDto.builder()
                .organizationId(organizationId)
                .definitions(definitions)
                .build();
    }

    /**
     * Catalog definitions for the user's membership organization (main, else first active). Empty
     * when the user has no active membership. Used by self-service submission and class-name
     * resolution.
     */
    @Transactional(readOnly = true)
    public List<DrivingLicenseDefinitionDto> listAvailableDefinitionsForUser(Long userId) {
        return resolveMembershipOrganizationId(userId)
                .map(this::getAvailableDefinitionsForOrg)
                .map(OrgDrivingLicenseCatalogDto::getDefinitions)
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
    public void requireDefinitionAvailableForOrg(
            Long tenantDrivingLicenseDefinitionId, Long organizationId) {
        Organization organization = requireOrganization(organizationId);
        TenantDrivingLicenseDefinition definition =
                tenantDefinitionRepository
                        .findById(tenantDrivingLicenseDefinitionId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Driving license definition not found: "
                                                        + tenantDrivingLicenseDefinitionId));
        if (!LifecycleFields.isCatalogSelectable(definition)) {
            throw new ConflictException("Driving license definition is not available");
        }
        Set<Long> catalogIds =
                resolveCatalogDefinitions(organization).stream()
                        .map(DrivingLicenseDefinitionDto::getId)
                        .collect(Collectors.toSet());
        if (!catalogIds.contains(tenantDrivingLicenseDefinitionId)) {
            throw new ConflictException(
                    "Driving license definition is not available for organization "
                            + organizationId);
        }
    }

    private List<DrivingLicenseDefinitionDto> resolveCatalogDefinitions(Organization organization) {
        Optional<Standard> standard =
                effectiveStandardResolver.resolveForOrganization(organization);
        if (standard.isEmpty()) {
            return List.of();
        }
        Set<String> selectedKeys =
                selectionRepository.findByStandardId(standard.get().getId()).stream()
                        .map(DrivingLicenseDefinitionSelection::getDefinitionConfigKey)
                        .collect(Collectors.toSet());
        if (selectedKeys.isEmpty()) {
            return List.of();
        }
        return tenantDefinitionRepository.findByConfigKeyIn(selectedKeys).stream()
                .filter(definition -> !definition.isArchived())
                .map(DrivingLicenseCatalogService::toDefinitionDto)
                .toList();
    }

    private Organization requireOrganization(Long organizationId) {
        return organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }
}
