/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.cache;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.cache.CacheNames;
import com.emberone.platform.cache.value.EffectiveStandardIdCacheValue;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.standard.repository.StandardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EffectiveStandardOrganizationCache {

    private final OrganizationRepository organizationRepository;
    private final StandardRepository standardRepository;
    private final EffectiveStandardTypeCache effectiveStandardTypeCache;

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheNames.EFFECTIVE_STANDARD_BY_ORG,
            key = "#tenantId + ':' + #organizationId")
    public EffectiveStandardIdCacheValue loadCached(Long tenantId, Long organizationId) {
        Organization organization =
                organizationRepository
                        .findById(organizationId)
                        .filter(org -> tenantId.equals(org.getTenantId()))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Organization not found: " + organizationId));

        if (organization.getStandardId() != null) {
            Long standardId = organization.getStandardId();
            if (standardRepository
                    .findById(standardId)
                    .filter(standard -> tenantId.equals(standard.getTenantId()))
                    .isEmpty()) {
                throw new IllegalStateException(
                        "Override standard "
                                + standardId
                                + " not found for organization "
                                + organizationId);
            }
            return new EffectiveStandardIdCacheValue(standardId);
        }
        return effectiveStandardTypeCache.loadCached(tenantId, organization.getType().getId());
    }
}
