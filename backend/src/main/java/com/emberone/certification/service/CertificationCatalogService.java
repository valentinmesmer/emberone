/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.service;

import com.emberone.certification.dto.CertificationDefinitionDto;
import com.emberone.certification.dto.OrgCertificationCatalogDto;
import com.emberone.certification.standard.CertDefinitionSelection;
import com.emberone.certification.standard.CertDefinitionSelectionRepository;
import com.emberone.certification.tenant.TenantCertDefinition;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.service.EffectiveStandardResolver;
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
public class CertificationCatalogService {

    private final OrganizationRepository organizationRepository;
    private final EffectiveStandardResolver effectiveStandardResolver;
    private final CertDefinitionSelectionRepository selectionRepository;
    private final TenantCertDefinitionRepository tenantDefinitionRepository;
    private final TenantCertDefinitionEvidenceTypeService evidenceTypeService;

    CertificationDefinitionDto toDefinitionDto(
            TenantCertDefinition definition, Set<String> evidenceTypes) {
        return CertificationDefinitionDto.builder()
                .id(definition.getId())
                .name(definition.getName())
                .description(definition.getDescription())
                .intervalDays(definition.getIntervalDays())
                .reminderDaysBeforeExpiry(definition.getReminderDaysBeforeExpiry())
                .active(definition.isActive())
                .configKey(definition.getConfigKey())
                .evidenceTypes(evidenceTypes)
                .build();
    }

    @Transactional(readOnly = true)
    public OrgCertificationCatalogDto getCatalog(Long organizationId) {
        Organization organization = requireOrganization(organizationId);
        List<CertificationDefinitionDto> definitions =
                resolveCatalogDefinitions(organization).stream()
                        .sorted(
                                Comparator.comparing(
                                        CertificationDefinitionDto::getName,
                                        String.CASE_INSENSITIVE_ORDER))
                        .toList();
        return OrgCertificationCatalogDto.builder()
                .organizationId(organizationId)
                .definitions(definitions)
                .build();
    }

    @Transactional(readOnly = true)
    public void requireTypeAvailableForOrg(Long tenantCertDefinitionId, Long organizationId) {
        Organization organization = requireOrganization(organizationId);
        TenantCertDefinition definition =
                tenantDefinitionRepository
                        .findById(tenantCertDefinitionId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Certification definition not found: "
                                                        + tenantCertDefinitionId));
        if (!LifecycleFields.isCatalogSelectable(definition)) {
            throw new ConflictException("Certification definition is not available");
        }
        Set<Long> catalogIds =
                resolveCatalogDefinitions(organization).stream()
                        .map(CertificationDefinitionDto::getId)
                        .collect(Collectors.toSet());
        if (!catalogIds.contains(tenantCertDefinitionId)) {
            throw new ConflictException(
                    "Certification definition is not available for organization " + organizationId);
        }
    }

    private List<CertificationDefinitionDto> resolveCatalogDefinitions(Organization organization) {
        Optional<Standard> standard =
                effectiveStandardResolver.resolveForOrganization(organization);
        if (standard.isEmpty()) {
            return List.of();
        }
        Set<String> selectedKeys =
                selectionRepository.findByStandardId(standard.get().getId()).stream()
                        .map(CertDefinitionSelection::getDefinitionConfigKey)
                        .collect(Collectors.toSet());
        if (selectedKeys.isEmpty()) {
            return List.of();
        }
        return tenantDefinitionRepository.findByConfigKeyIn(selectedKeys).stream()
                .filter(definition -> !definition.isArchived())
                .map(
                        definition ->
                                toDefinitionDto(
                                        definition,
                                        TenantCertDefinitionEvidenceTypeService.toStringSet(
                                                evidenceTypeService.getEvidenceTypes(
                                                        definition.getId()))))
                .toList();
    }

    private Organization requireOrganization(Long organizationId) {
        return organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }
}
