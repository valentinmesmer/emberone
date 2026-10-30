/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.cache;

import com.emberone.platform.cache.CacheNames;
import com.emberone.platform.cache.value.EffectiveStandardIdCacheValue;
import com.emberone.standard.repository.StandardAssignmentRepository;
import com.emberone.standard.repository.StandardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EffectiveStandardTypeCache {

    private final StandardAssignmentRepository assignmentRepository;
    private final StandardRepository standardRepository;

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheNames.EFFECTIVE_STANDARD_BY_ORG_TYPE,
            key = "#tenantId + ':' + #organizationTypeId",
            unless = "#result == null")
    public EffectiveStandardIdCacheValue loadCached(Long tenantId, Long organizationTypeId) {
        return assignmentRepository
                .findByTenantIdAndOrganizationType_Id(tenantId, organizationTypeId)
                .map(
                        assignment -> {
                            Long standardId = assignment.getStandardId();
                            if (standardRepository
                                    .findById(standardId)
                                    .filter(standard -> tenantId.equals(standard.getTenantId()))
                                    .isEmpty()) {
                                throw new IllegalStateException(
                                        "Assigned standard "
                                                + standardId
                                                + " not found for type "
                                                + organizationTypeId);
                            }
                            return new EffectiveStandardIdCacheValue(standardId);
                        })
                .orElse(null);
    }

    @CacheEvict(
            cacheNames = CacheNames.EFFECTIVE_STANDARD_BY_ORG_TYPE,
            key = "#tenantId + ':' + #organizationTypeId")
    public void evict(Long tenantId, Long organizationTypeId) {}
}
