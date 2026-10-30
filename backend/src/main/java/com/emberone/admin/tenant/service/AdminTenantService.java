/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.service;

import com.emberone.admin.organization.service.AdminOrganizationService;
import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.admin.tenant.domain.Tenant;
import com.emberone.admin.tenant.dto.CreateTenantRequest;
import com.emberone.admin.tenant.dto.TenantAdminDetailDto;
import com.emberone.admin.tenant.dto.TenantAdminPatchRequest;
import com.emberone.admin.tenant.dto.TenantDto;
import com.emberone.admin.tenant.event.TenantUpdatedEvent;
import com.emberone.admin.tenant.list.TenantListDefinition;
import com.emberone.admin.tenant.list.TenantListFilter;
import com.emberone.admin.tenant.list.TenantListSpecs;
import com.emberone.admin.tenant.mapper.TenantMapper;
import com.emberone.admin.tenant.repository.TenantRepository;
import com.emberone.admin.tenant.util.TenantAbbreviation;
import com.emberone.federalstate.FederalStateSettingsRegistry;
import com.emberone.federalstate.domain.FederalState;
import com.emberone.organization.dto.SetupOrganizationRequest;
import com.emberone.organization.service.OrganizationService;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.PageDto;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.standard.service.StandardReadinessService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Administrative tenant lifecycle and tenant master-data use cases. */
@Service
@RequiredArgsConstructor
public class AdminTenantService {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final OrganizationService organizationService;
    private final TokenService tokenService;
    private final TenantCreationService tenantCreationService;
    private final TenantStandardsBootstrapService tenantStandardsBootstrapService;
    private final AdminOrganizationService adminOrganizationService;
    private final StandardReadinessService standardReadinessService;
    private final ApplicationEventPublisher eventPublisher;
    private final FederalStateSettingsRegistry federalStateSettingsRegistry;

    @Transactional(readOnly = true)
    public PageDto<TenantDto> listTenants(ListQuery query, TenantListFilter filter) {
        Specification<Tenant> visibility = tenantVisibilitySpec();
        Pageable pageable = TenantListDefinition.INSTANCE.pageable(query);
        Specification<Tenant> spec =
                Specification.allOf(
                        visibility,
                        TenantListDefinition.INSTANCE.searchSpecification(query),
                        TenantListSpecs.matching(filter));
        Page<Tenant> page = tenantRepository.findAll(spec, pageable);
        return PageDto.from(page, page.getContent().stream().map(tenantMapper::toDto).toList());
    }

    private Specification<Tenant> tenantVisibilitySpec() {
        if (tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())) {
            return Specification.unrestricted();
        }
        if (tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())) {
            Long tenantId = tokenService.getTenantId();
            if (tenantId == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing tenant context");
            }
            return (root, query, cb) -> cb.equal(root.get("id"), tenantId);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized");
    }

    @Transactional(readOnly = true)
    public TenantAdminDetailDto getTenantDetail(long tenantId) {
        return toDetail(requireTenant(tenantId));
    }

    public List<FederalState> listConfiguredFederalStates() {
        return federalStateSettingsRegistry.configuredStates();
    }

    public TenantDto createTenant(CreateTenantRequest request) {
        String abbreviation = TenantAbbreviation.requireValid(request.getAbbreviation());
        if (tenantRepository.existsByAbbreviation(abbreviation)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant code already exists");
        }
        FederalState federalState =
                request.getFederalState() == null ? FederalState.BAYERN : request.getFederalState();
        federalStateSettingsRegistry.require(federalState);
        Tenant tenant =
                Tenant.builder()
                        .name(request.getName().trim())
                        .abbreviation(abbreviation)
                        .locale(
                                request.getLocale() == null
                                        ? SupportedLocale.DE
                                        : request.getLocale())
                        .federalState(federalState)
                        .build();
        Tenant saved = tenantCreationService.persist(tenant);
        tenantStandardsBootstrapService.syncBaseline(saved.getId());
        return tenantMapper.toDto(saved);
    }

    /** Re-applies the federal-state baseline to an existing tenant (e.g. after a release). */
    public void syncBaseline(long tenantId) {
        requireTenant(tenantId);
        tenantStandardsBootstrapService.syncBaseline(tenantId);
    }

    /**
     * Seed the read-only system baseline standard for a newly created tenant. Tenants must clone it
     * and assign their own copy before new organizations receive materialized setup content.
     */
    @Transactional
    public TenantAdminDetailDto patchTenant(long tenantId, TenantAdminPatchRequest request) {
        Tenant tenant = requireTenant(tenantId);
        applyPatch(tenant, request);
        Tenant saved = tenantRepository.save(tenant);
        eventPublisher.publishEvent(new TenantUpdatedEvent(saved.getId()));
        return toDetail(saved);
    }

    @Transactional
    public TenantAdminDetailDto createRootOrganization(
            long tenantId, SetupOrganizationRequest request) {
        requireTenant(tenantId);
        organizationService.createRootOrganizationForTenant(tenantId, request);
        return getTenantDetail(tenantId);
    }

    private Tenant requireTenant(long tenantId) {
        return tenantRepository
                .findById(tenantId)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Tenant not found"));
    }

    private void applyPatch(Tenant tenant, TenantAdminPatchRequest request) {
        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name cannot be empty");
            }
            tenant.setName(name);
        }
    }

    private TenantAdminDetailDto toDetail(Tenant tenant) {
        return TenantAdminDetailDto.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .abbreviation(tenant.getAbbreviation())
                .locale(tenant.getLocale())
                .federalState(tenant.getFederalState())
                .rootOrganizations(adminOrganizationService.getRootOrganizationSummaries())
                .standardsConfiguredForOrgCreation(
                        standardReadinessService.isReadyForOrganizationCreation(tenant.getId()))
                .build();
    }
}
