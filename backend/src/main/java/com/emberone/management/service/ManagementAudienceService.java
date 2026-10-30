/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.service;

import com.emberone.management.domain.ManagementAudience;
import com.emberone.management.domain.ManagementAudienceKind;
import com.emberone.management.dto.ManagementAudienceEntryDto;
import com.emberone.management.dto.ManagementAudienceEntryRequest;
import com.emberone.management.dto.ManagementAudienceSettingsDto;
import com.emberone.management.dto.ManagementOptionDto;
import com.emberone.management.exception.ManagementAudienceValidationException;
import com.emberone.management.repository.ManagementAudienceRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organization.repository.OrganizationTypeDefinitionRepository;
import com.emberone.organization.service.OrganizationTypeDefinitionService;
import com.emberone.permission.domain.PermissionType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configures which organizations a managing organization may open. Only configuration: nothing here
 * grants access, the access check reads these entries together with open activations.
 */
@Service
@RequiredArgsConstructor
public class ManagementAudienceService {

    private final ManagementAudienceRepository audienceRepository;
    private final ManagementCapabilityService capabilityService;
    private final OrganizationRepository organizationRepository;
    private final OrganizationTypeDefinitionRepository organizationTypeRepository;
    private final OrganizationTypeDefinitionService organizationTypeService;

    @Transactional(readOnly = true)
    public ManagementAudienceSettingsDto getSettings(Long tenantId, Long organizationId) {
        return toSettings(requireOrganization(tenantId, organizationId));
    }

    /**
     * Replaces the audience. A kept entry (same kind and reference) keeps its id and takes the new
     * permission types and duration. Removed entries are deleted.
     */
    @Transactional
    public ManagementAudienceSettingsDto replace(
            Long tenantId, Long organizationId, List<ManagementAudienceEntryRequest> entries) {
        Organization managing = requireOrganization(tenantId, organizationId);
        if (!capabilityService.isCapable(managing)) {
            throw new ManagementAudienceValidationException(
                    "The organization cannot manage other organizations");
        }
        validate(managing, entries);

        Map<String, ManagementAudience> existing =
                audienceRepository.findByManagingOrganizationIdOrderByIdAsc(organizationId).stream()
                        .collect(Collectors.toMap(entry -> entryKey(entry), Function.identity()));
        List<ManagementAudience> next = new ArrayList<>();
        for (ManagementAudienceEntryRequest request : entries) {
            ManagementAudience entry = existing.remove(entryKey(request));
            if (entry == null) {
                entry =
                        ManagementAudience.builder()
                                .tenantId(tenantId)
                                .managingOrganizationId(organizationId)
                                .kind(request.getKind())
                                .refId(request.getRefId())
                                .build();
            }
            entry.setPermissionTypes(
                    ManagementActivationService.encode(
                            EnumSet.copyOf(request.getPermissionTypes())));
            entry.setMaxDurationMinutes(request.getMaxDurationMinutes());
            next.add(entry);
        }
        audienceRepository.deleteAll(existing.values());
        audienceRepository.saveAll(next);
        return toSettings(managing);
    }

    private void validate(Organization managing, List<ManagementAudienceEntryRequest> entries) {
        Long tenantId = managing.getTenantId();
        Set<String> seen = new HashSet<>();
        for (ManagementAudienceEntryRequest entry : entries) {
            ManagementAudienceKind kind = entry.getKind();
            if (kind.needsReference() != (entry.getRefId() != null)) {
                throw new ManagementAudienceValidationException(
                        kind.needsReference()
                                ? "Audience " + kind + " needs a reference"
                                : "Audience " + kind + " takes no reference");
            }
            if (!seen.add(entryKey(entry))) {
                throw new ManagementAudienceValidationException("Duplicate audience entry");
            }
            requirePermissionTypes(entry.getPermissionTypes());
            requireUsableReference(managing, kind, entry.getRefId());
        }
    }

    /**
     * At least one type, and never {@code MANAGEMENT_ACCESS}: otherwise the target could open more.
     */
    private static void requirePermissionTypes(List<PermissionType> types) {
        if (types == null || types.isEmpty()) {
            throw new ManagementAudienceValidationException("Audience needs permission types");
        }
        if (types.contains(PermissionType.MANAGEMENT_ACCESS)) {
            throw new ManagementAudienceValidationException(
                    "Management access cannot be an audience permission");
        }
    }

    /**
     * The reference must be active (organization or organization type). A direct {@code ORG} entry
     * on the managing organization itself is rejected. Subtree and type entries that also cover it
     * are allowed: the activation excludes the managing organization at runtime.
     */
    private void requireUsableReference(
            Organization managing, ManagementAudienceKind kind, Long refId) {
        if (kind == ManagementAudienceKind.TENANT) {
            return;
        }
        Long tenantId = managing.getTenantId();
        List<Organization> activeOrganizations =
                organizationRepository.findActiveByTenantIdOrderByName(tenantId);
        boolean usable =
                switch (kind) {
                    case ORG, ORG_SUBTREE ->
                            activeOrganizations.stream().anyMatch(org -> org.getId().equals(refId));
                    case ORG_TYPE ->
                            organizationTypeService.listActive().stream()
                                    .anyMatch(type -> type.getId().equals(refId));
                    case TENANT -> true;
                };
        if (!usable) {
            throw new ManagementAudienceValidationException(
                    "Audience reference is unknown or inactive: " + kind + " " + refId);
        }
        if (kind == ManagementAudienceKind.ORG && refId.equals(managing.getId())) {
            throw new ManagementAudienceValidationException("An organization cannot manage itself");
        }
    }

    private ManagementAudienceSettingsDto toSettings(Organization managing) {
        Long tenantId = managing.getTenantId();
        return ManagementAudienceSettingsDto.builder()
                .entries(
                        audienceRepository
                                .findByManagingOrganizationIdOrderByIdAsc(managing.getId())
                                .stream()
                                .map(ManagementAudienceService::toEntryDto)
                                .toList())
                .organizations(
                        organizationRepository.findActiveByTenantIdOrderByName(tenantId).stream()
                                .map(org -> option(org.getId(), org.getName()))
                                .toList())
                .organizationTypes(
                        organizationTypeService.listActive().stream()
                                .map(type -> option(type.getId(), type.getName()))
                                .toList())
                .build();
    }

    private Organization requireOrganization(Long tenantId, Long organizationId) {
        return organizationRepository
                .findById(organizationId)
                .filter(org -> tenantId.equals(org.getTenantId()))
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }

    private static String entryKey(ManagementAudience entry) {
        return entryKey(entry.getKind(), entry.getRefId());
    }

    private static String entryKey(ManagementAudienceEntryRequest request) {
        return entryKey(request.getKind(), request.getRefId());
    }

    private static String entryKey(ManagementAudienceKind kind, Long refId) {
        return kind + ":" + refId;
    }

    private static ManagementAudienceEntryDto toEntryDto(ManagementAudience entry) {
        return ManagementAudienceEntryDto.builder()
                .id(entry.getId())
                .kind(entry.getKind())
                .refId(entry.getRefId())
                .permissionTypes(ManagementActivationService.decode(entry.getPermissionTypes()))
                .maxDurationMinutes(entry.getMaxDurationMinutes())
                .build();
    }

    private static ManagementOptionDto option(Long id, String name) {
        return ManagementOptionDto.builder().id(id).name(name).build();
    }
}
