/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.organizationtype.service;

import com.emberone.admin.organizationtype.dto.CreateOrganizationTypeRequest;
import com.emberone.admin.organizationtype.dto.OrganizationTypeDto;
import com.emberone.admin.organizationtype.dto.UpdateOrganizationTypeRequest;
import com.emberone.organization.domain.OrganizationTypeDefinition;
import com.emberone.organization.service.OrganizationTypeDefinitionService;
import com.emberone.platform.security.TenantScopeGuard;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tenant admin facade for the organization type catalog. Binds the tenant from the path so RLS
 * resolves to the right tenant.
 */
@Service
@RequiredArgsConstructor
public class AdminOrganizationTypeService {

    private final OrganizationTypeDefinitionService organizationTypeDefinitionService;

    private static OrganizationTypeDto toDto(OrganizationTypeDefinition definition) {
        return OrganizationTypeDto.builder()
                .id(definition.getId())
                .name(definition.getName())
                .active(definition.isActive())
                .build();
    }

    @Transactional
    public List<OrganizationTypeDto> listTypes(long tenantId) {
        return withTenantContext(
                tenantId,
                () ->
                        organizationTypeDefinitionService.listAll().stream()
                                .map(AdminOrganizationTypeService::toDto)
                                .toList());
    }

    @Transactional
    public OrganizationTypeDto createType(long tenantId, CreateOrganizationTypeRequest request) {
        return withTenantContext(
                tenantId,
                () -> toDto(organizationTypeDefinitionService.create(tenantId, request.getName())));
    }

    @Transactional
    public OrganizationTypeDto updateType(
            long tenantId, Long id, UpdateOrganizationTypeRequest request) {
        return withTenantContext(
                tenantId,
                () ->
                        toDto(
                                organizationTypeDefinitionService.update(
                                        tenantId, id, request.getName(), request.getActive())));
    }

    @Transactional
    public void deleteType(long tenantId, Long id) {
        withTenantContext(
                tenantId,
                () -> {
                    organizationTypeDefinitionService.delete(tenantId, id);
                    return null;
                });
    }

    private <T> T withTenantContext(long tenantId, Supplier<T> action) {
        TenantScopeGuard.bind(tenantId);
        TenantScopeGuard.requireActiveTenantMatches(tenantId);
        return action.get();
    }
}
