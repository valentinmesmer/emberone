/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.service;

import com.emberone.admin.tenant.domain.Tenant;
import com.emberone.admin.tenant.repository.TenantRepository;
import com.emberone.management.domain.ManagementActivation;
import com.emberone.management.domain.ManagementActivationStatus;
import com.emberone.management.dto.ManagementActivationLogItemDto;
import com.emberone.management.exception.ManagementActivationNotFoundException;
import com.emberone.management.list.ManagementActivationListDefinition;
import com.emberone.management.list.ManagementActivationListFilter;
import com.emberone.management.list.ManagementActivationListSpecs;
import com.emberone.management.repository.ManagementActivationRepository;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.PageDto;
import com.emberone.platform.web.list.ListQuery;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Read-only log of who opened which organization when, and why. Admin reads span all tenants for
 * global admins (cross-tenant read policy); tenant admins and the organization view stay inside one
 * tenant through RLS.
 */
@Service
@RequiredArgsConstructor
public class ManagementActivationLogService {

    private final ManagementActivationRepository activationRepository;
    private final TenantRepository tenantRepository;
    private final TokenService tokenService;

    /** Admin log: all tenants for global admins, the own tenant for tenant admins. */
    @Transactional(readOnly = true)
    public PageDto<ManagementActivationLogItemDto> listForAdmin(
            ListQuery query, ManagementActivationListFilter filter) {
        ManagementActivationListFilter scoped = filter;
        if (!tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())) {
            if (!tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized");
            }
            Long tenantId = tokenService.getTenantId();
            if (tenantId == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing tenant context");
            }
            for (Long requested : filter.tenantIds()) {
                if (!Objects.equals(requested, tenantId)) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN, "Cannot filter outside your tenant");
                }
            }
            scoped =
                    new ManagementActivationListFilter(
                            List.of(tenantId),
                            filter.statuses(),
                            filter.organizationIds(),
                            filter.managingOrganizationIds());
        }
        return page(ManagementActivationListSpecs.search(query.search()), query, scoped);
    }

    /**
     * One entry of the admin log. Tenant scope follows RLS, so a tenant admin never finds foreign
     * ids.
     */
    @Transactional(readOnly = true)
    public ManagementActivationLogItemDto getForAdmin(long activationId) {
        ManagementActivation activation =
                activationRepository
                        .findWithDetailsById(activationId)
                        .orElseThrow(() -> new ManagementActivationNotFoundException(activationId));
        String tenantName =
                tenantRepository.findById(activation.getTenantId()).map(Tenant::getName).orElse("");
        return toDto(activation, Instant.now(), Map.of(activation.getTenantId(), tenantName));
    }

    /** Same log, limited to one target organization. */
    @Transactional(readOnly = true)
    public PageDto<ManagementActivationLogItemDto> listForTarget(
            long targetOrganizationId, ListQuery query, ManagementActivationListFilter filter) {
        return page(
                ManagementActivationListSpecs.forTarget(targetOrganizationId)
                        .and(ManagementActivationListSpecs.search(query.search())),
                query,
                filter);
    }

    private PageDto<ManagementActivationLogItemDto> page(
            Specification<ManagementActivation> base,
            ListQuery query,
            ManagementActivationListFilter filter) {
        Instant now = Instant.now();
        Page<ManagementActivation> page =
                activationRepository.findAll(
                        base.and(ManagementActivationListSpecs.matching(filter, now)),
                        ManagementActivationListDefinition.INSTANCE.pageable(query));
        Map<Long, String> tenantNames =
                tenantRepository
                        .findAllById(
                                page.getContent().stream()
                                        .map(ManagementActivation::getTenantId)
                                        .collect(Collectors.toSet()))
                        .stream()
                        .collect(Collectors.toMap(Tenant::getId, Tenant::getName));
        return PageDto.from(
                page, page.getContent().stream().map(a -> toDto(a, now, tenantNames)).toList());
    }

    private static ManagementActivationLogItemDto toDto(
            ManagementActivation a, Instant now, Map<Long, String> tenantNames) {
        return ManagementActivationLogItemDto.builder()
                .id(a.getId())
                .tenantId(a.getTenantId())
                .tenantName(tenantNames.getOrDefault(a.getTenantId(), ""))
                .userId(a.getUserId())
                .userFirstName(a.getUser() == null ? null : a.getUser().getFirstName())
                .userLastName(a.getUser() == null ? null : a.getUser().getLastName())
                .managingOrganizationId(a.getManagingOrganizationId())
                .managingOrganizationName(
                        a.getManagingOrganization() == null
                                ? null
                                : a.getManagingOrganization().getName())
                .targetOrganizationId(a.getTargetOrganizationId())
                .targetOrganizationName(
                        a.getTargetOrganization() == null
                                ? null
                                : a.getTargetOrganization().getName())
                .activatedAt(a.getActivatedAt())
                .validUntil(a.getValidUntil())
                .endedAt(a.getEndedAt())
                .endedBy(a.getEndedBy())
                .status(ManagementActivationStatus.of(a, now))
                .permissionTypes(ManagementActivationService.decode(a.getPermissionTypes()))
                .reason(a.getReason())
                .build();
    }
}
