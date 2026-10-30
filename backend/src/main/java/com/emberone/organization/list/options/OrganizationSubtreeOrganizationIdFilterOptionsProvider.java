/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.list.options;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organization.service.OrganizationSubtreeSupport;
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

/**
 * Active organizations in the subtree of the route organization (incl. root), for org-workspace
 * lists that filter by {@code organizationId}.
 */
@Component
@RequiredArgsConstructor
public class OrganizationSubtreeOrganizationIdFilterOptionsProvider
        implements ListFilterOptionsProvider {

    private final OrganizationRepository organizationRepository;
    private final ListFilterOptionsAuthorization listFilterOptionsAuthorization;

    @Override
    public String key() {
        return "organizationId";
    }

    @Override
    public Set<ListFilterOptionsScope> scopes() {
        return Set.of(ListFilterOptionsScope.ORGANIZATION);
    }

    @Override
    public List<ListFilterOptionDto> resolve(ListFilterOptionsContext context) {
        long rootOrganizationId = context.requireOrganizationId();
        Organization root =
                organizationRepository
                        .findById(rootOrganizationId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Organization not found"));
        List<Organization> inTenant = organizationRepository.findAllWithParentFetch();
        Set<Long> subtreeIds =
                OrganizationSubtreeSupport.collectSubtreeIds(rootOrganizationId, inTenant);
        var permission = context.effectiveOrganizationFilterPermission();
        return inTenant.stream()
                .filter(organization -> subtreeIds.contains(organization.getId()))
                .filter(organization -> !organization.isDeactivated())
                .filter(
                        organization ->
                                listFilterOptionsAuthorization
                                        .mayExposeOrganizationForOrgWorkspaceFilter(
                                                organization.getId(), permission))
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
