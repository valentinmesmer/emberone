/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.service;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.qualification.dto.QualificationDefinitionDto;
import com.emberone.qualification.standard.QualificationSelection;
import com.emberone.qualification.standard.QualificationSelectionRepository;
import com.emberone.qualification.tenant.TenantQualificationDefinition;
import com.emberone.qualification.tenant.TenantQualificationDefinitionRepository;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.service.EffectiveStandardResolver;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QualificationCatalogService {

    private final OrganizationRepository organizationRepository;
    private final EffectiveStandardResolver effectiveStandardResolver;
    private final QualificationSelectionRepository selectionRepository;
    private final TenantQualificationDefinitionRepository definitionRepository;

    @Transactional(readOnly = true)
    public void requireAvailableForOrg(Long tenantQualificationDefinitionId, Long organizationId) {
        Organization organization = requireOrganization(organizationId);
        TenantQualificationDefinition definition =
                definitionRepository
                        .findById(tenantQualificationDefinitionId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Qualification definition not found: "
                                                        + tenantQualificationDefinitionId));
        if (!LifecycleFields.isCatalogSelectable(definition)) {
            throw new ConflictException("Qualification definition is not available");
        }
        Set<Long> catalogIds =
                resolveCatalogDefinitions(organization).stream()
                        .map(QualificationDefinitionDto::getId)
                        .collect(Collectors.toSet());
        if (!catalogIds.contains(tenantQualificationDefinitionId)) {
            throw new ConflictException(
                    "Qualification definition is not available for organization " + organizationId);
        }
    }

    @Transactional(readOnly = true)
    public List<QualificationDefinitionDto> resolveCatalogForOrganization(Long organizationId) {
        Organization organization = requireOrganization(organizationId);
        return resolveCatalogDefinitions(organization);
    }

    private List<QualificationDefinitionDto> resolveCatalogDefinitions(Organization organization) {
        Optional<Standard> standard =
                effectiveStandardResolver.resolveForOrganization(organization);
        if (standard.isEmpty()) {
            return List.of();
        }
        Set<String> selectedKeys =
                selectionRepository.findByStandardId(standard.get().getId()).stream()
                        .map(QualificationSelection::getDefinitionConfigKey)
                        .collect(Collectors.toSet());
        if (selectedKeys.isEmpty()) {
            return List.of();
        }
        return definitionRepository.findByConfigKeyIn(selectedKeys.stream().toList()).stream()
                .filter(LifecycleFields::isCatalogSelectable)
                .map(TenantQualificationDefinitionService::toDefinitionDto)
                .toList();
    }

    private Organization requireOrganization(Long organizationId) {
        return organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }
}
