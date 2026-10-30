/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.list.options;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.options.ListFilterOptionDto;
import com.emberone.platform.web.list.options.ListFilterOptionsAuthorization;
import com.emberone.platform.web.list.options.ListFilterOptionsContext;
import com.emberone.platform.web.list.options.ListFilterOptionsProvider;
import com.emberone.platform.web.list.options.ListFilterOptionsScope;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class AdminOrganizationIdFilterOptionsProvider implements ListFilterOptionsProvider {

    private final OrganizationRepository organizationRepository;
    private final TokenService tokenService;
    private final ListFilterOptionsAuthorization listFilterOptionsAuthorization;

    @Override
    public String key() {
        return "organizationId";
    }

    @Override
    public Set<ListFilterOptionsScope> scopes() {
        return Set.of(ListFilterOptionsScope.ADMIN);
    }

    @Override
    public List<ListFilterOptionDto> resolve(ListFilterOptionsContext context) {
        List<Organization> organizations;
        boolean globalAdmin = tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName());
        if (globalAdmin) {
            organizations = organizationRepository.findAllActiveOrderByName();
        } else if (tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())) {
            Long tenantId = tokenService.getTenantId();
            if (tenantId == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing tenant context");
            }
            organizations = organizationRepository.findActiveByTenantIdOrderByName(tenantId);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized");
        }
        return organizations.stream()
                .filter(
                        organization ->
                                globalAdmin
                                        || listFilterOptionsAuthorization
                                                .mayExposeOrganizationForAdminUserListFilter(
                                                        organization.getId()))
                .map(
                        organization ->
                                new ListFilterOptionDto(
                                        String.valueOf(organization.getId()),
                                        organization.getName()))
                .sorted(
                        Comparator.comparing(
                                ListFilterOptionDto::label, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
