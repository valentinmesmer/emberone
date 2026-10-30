/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.cache;

import com.emberone.platform.cache.CacheNames;
import com.emberone.platform.cache.value.ActiveOrganizationMembershipCacheValue;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActiveOrganizationMembershipCache {

    private final UserOrganizationMembershipRepository membershipRepository;

    @Cacheable(
            cacheNames = CacheNames.ACTIVE_ORG_MEMBERSHIP,
            key = "#userId + ':' + #organizationId")
    public ActiveOrganizationMembershipCacheValue loadCached(Long userId, Long organizationId) {
        boolean active =
                membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        userId, organizationId, Instant.now());
        return new ActiveOrganizationMembershipCacheValue(active);
    }
}
