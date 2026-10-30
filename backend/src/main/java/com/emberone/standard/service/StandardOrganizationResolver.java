/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.service;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.standard.cache.StandardOrganizationIdCache;
import com.emberone.standard.domain.Standard;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves which organizations are currently driven by a standard template. Kept separate from
 * {@link StandardMaterializationService} so standard-domain SPI beans can query org scope without
 * creating a dependency cycle through {@link com.emberone.standard.spi.StandardDomainRegistry}.
 */
@Service
@RequiredArgsConstructor
public class StandardOrganizationResolver {

    private final StandardOrganizationIdCache standardOrganizationIdCache;
    private final OrganizationRepository organizationRepository;

    /**
     * Organizations of every org type assigned this standard that inherit (no override), plus
     * organizations whose explicit per-org override points at this standard.
     */
    @Transactional(readOnly = true)
    public List<Organization> resolveOrganizationsForStandard(Standard standard) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || !tenantId.equals(standard.getTenantId())) {
            return List.of();
        }
        List<Long> organizationIds =
                standardOrganizationIdCache
                        .loadCached(tenantId, standard.getId())
                        .organizationIds();
        if (organizationIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Organization> byId =
                organizationRepository.findAllById(organizationIds).stream()
                        .collect(Collectors.toMap(Organization::getId, Function.identity()));
        List<Organization> result = new ArrayList<>(organizationIds.size());
        for (Long organizationId : organizationIds) {
            Organization organization = byId.get(organizationId);
            if (organization != null) {
                result.add(organization);
            }
        }
        return result;
    }
}
