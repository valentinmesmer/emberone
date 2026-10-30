/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.cache;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.cache.CacheNames;
import com.emberone.platform.cache.value.OrganizationIdsForStandardCacheValue;
import com.emberone.standard.repository.StandardAssignmentRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StandardOrganizationIdCache {

    private final StandardAssignmentRepository assignmentRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheNames.ORGANIZATIONS_FOR_STANDARD,
            key = "#tenantId + ':' + #standardId")
    public OrganizationIdsForStandardCacheValue loadCached(Long tenantId, Long standardId) {
        Map<Long, Long> orderedIds = new LinkedHashMap<>();
        for (var assignment :
                assignmentRepository.findAllByTenantIdAndStandardId(tenantId, standardId)) {
            for (Organization org :
                    organizationRepository.findByType_IdAndStandardIdIsNull(
                            assignment.getOrganizationType().getId())) {
                orderedIds.put(org.getId(), org.getId());
            }
        }
        for (Organization org :
                organizationRepository.findByTenantIdAndStandardId(tenantId, standardId)) {
            orderedIds.put(org.getId(), org.getId());
        }
        return new OrganizationIdsForStandardCacheValue(new ArrayList<>(orderedIds.keySet()));
    }

    @CacheEvict(
            cacheNames = CacheNames.ORGANIZATIONS_FOR_STANDARD,
            key = "#tenantId + ':' + #standardId")
    public void evict(Long tenantId, Long standardId) {}
}
