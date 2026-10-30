/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.service;

import com.emberone.federalstate.baseline.TenantBaseline;
import com.emberone.organization.domain.OrganizationTypeDefinition;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organization.repository.OrganizationTypeDefinitionRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.standard.repository.StandardAssignmentRepository;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tenant-scoped catalog of organization type kinds (e.g. "Abteilung", "Team"), referenced by id.
 */
@Service
@RequiredArgsConstructor
public class OrganizationTypeDefinitionService {

    private final OrganizationTypeDefinitionRepository repository;
    private final OrganizationRepository organizationRepository;
    private final StandardAssignmentRepository standardAssignmentRepository;

    /**
     * Inserts baseline organization types for a tenant when missing. Existing rows are left
     * unchanged.
     */
    @Transactional
    public void seedBaselineTypesIfMissing(
            long tenantId, List<TenantBaseline.OrganizationTypeDefinitionBaseline> items) {
        withTenantContext(
                tenantId,
                () -> {
                    for (TenantBaseline.OrganizationTypeDefinitionBaseline item : items) {
                        if (repository.findByConfigKey(item.configKey()).isPresent()) {
                            continue;
                        }
                        repository.save(
                                OrganizationTypeDefinition.builder()
                                        .tenantId(tenantId)
                                        .configKey(item.configKey())
                                        .name(item.name())
                                        .active(true)
                                        .build());
                    }
                    return null;
                });
    }

    @Transactional(readOnly = true)
    public List<OrganizationTypeDefinition> listAll() {
        return repository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<OrganizationTypeDefinition> listActive() {
        return listAll().stream().filter(OrganizationTypeDefinition::isActive).toList();
    }

    /** Resolve an organization type by id, requiring it to belong to the tenant and be active. */
    @Transactional(readOnly = true)
    public OrganizationTypeDefinition requireActiveType(Long organizationTypeId) {
        if (organizationTypeId == null) {
            throw new IllegalArgumentException("Organization type is required");
        }
        return repository
                .findByIdAndActiveTrue(organizationTypeId)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown or inactive organization type: "
                                                + organizationTypeId));
    }

    @Transactional
    public OrganizationTypeDefinition create(Long tenantId, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }
        String trimmedName = name.trim();
        if (repository.existsByNameIgnoreCase(trimmedName)) {
            throw new ConflictException(
                    "An organization type named '" + trimmedName + "' already exists");
        }
        return repository.save(
                OrganizationTypeDefinition.builder()
                        .tenantId(tenantId)
                        .name(trimmedName)
                        .active(true)
                        .build());
    }

    @Transactional
    public OrganizationTypeDefinition update(Long tenantId, Long id, String name, Boolean active) {
        OrganizationTypeDefinition definition = requireOwnedByTenant(tenantId, id);
        if (name != null && !name.isBlank()) {
            String trimmedName = name.trim();
            if (!definition.getName().equalsIgnoreCase(trimmedName)
                    && repository.existsByNameIgnoreCase(trimmedName)) {
                throw new ConflictException(
                        "An organization type named '" + trimmedName + "' already exists");
            }
            definition.setName(trimmedName);
        }
        if (active != null) {
            definition.setActive(active);
        }
        return repository.save(definition);
    }

    @Transactional
    public void delete(Long tenantId, Long id) {
        OrganizationTypeDefinition definition = requireOwnedByTenant(tenantId, id);
        boolean inUse =
                organizationRepository.existsByType_Id(id)
                        || standardAssignmentRepository.existsByOrganizationType_Id(id);
        if (inUse) {
            throw new ConflictException(
                    "Organization type '"
                            + definition.getName()
                            + "' is in use and cannot be deleted; deactivate it instead");
        }
        repository.delete(definition);
    }

    private OrganizationTypeDefinition requireOwnedByTenant(Long tenantId, Long id) {
        OrganizationTypeDefinition definition =
                repository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Organization type not found: " + id));
        if (!definition.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Organization type not found: " + id);
        }
        return definition;
    }

    private <T> T withTenantContext(long tenantId, Supplier<T> action) {
        TenantScopeGuard.bind(tenantId);
        TenantScopeGuard.requireActiveTenantMatches(tenantId);
        return action.get();
    }
}
