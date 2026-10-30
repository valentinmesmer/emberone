/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.service;

import com.emberone.organization.domain.Organization;
import com.emberone.platform.cache.value.EffectiveStandardIdCacheValue;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.standard.cache.EffectiveStandardOrganizationCache;
import com.emberone.standard.cache.EffectiveStandardTypeCache;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.repository.StandardRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves which {@link Standard} effectively applies to an organization or org type. Kept separate
 * from {@link StandardMaterializationService} so catalog/runtime services can query effective
 * standards without depending on the standard-domain materialization graph.
 */
@Service
@RequiredArgsConstructor
public class EffectiveStandardResolver {

    private final EffectiveStandardOrganizationCache effectiveStandardOrganizationCache;
    private final EffectiveStandardTypeCache effectiveStandardTypeCache;
    private final StandardRepository standardRepository;

    /**
     * Resolve the standard that effectively drives an organization: its explicit per-org override
     * when set, otherwise the standard assigned to its organization type.
     */
    @Transactional(readOnly = true)
    public Optional<Standard> resolveForOrganization(Organization organization) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || organization == null || organization.getId() == null) {
            return Optional.empty();
        }
        try {
            return loadStandard(
                    tenantId,
                    effectiveStandardOrganizationCache.loadCached(tenantId, organization.getId()));
        } catch (ResourceNotFoundException ex) {
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public Optional<Standard> resolveForOrganizationType(Long organizationTypeId) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return Optional.empty();
        }
        return loadStandard(
                tenantId, effectiveStandardTypeCache.loadCached(tenantId, organizationTypeId));
    }

    private Optional<Standard> loadStandard(Long tenantId, EffectiveStandardIdCacheValue cached) {
        if (cached == null) {
            return Optional.empty();
        }
        return standardRepository
                .findById(cached.standardId())
                .filter(standard -> tenantId.equals(standard.getTenantId()));
    }
}
