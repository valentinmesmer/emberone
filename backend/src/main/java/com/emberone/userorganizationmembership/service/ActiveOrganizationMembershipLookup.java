/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.userorganizationmembership.service;

import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves organizations where the user currently holds an active membership.
 *
 * <p>Self-service catalog selection pools (training courses, certifications, driving licenses)
 * union the effective-standard catalogs of these organizations. Org-admin write paths and workflow
 * context organization resolution stay on a single organization (typically the main membership).
 */
@Service
@RequiredArgsConstructor
public class ActiveOrganizationMembershipLookup {

    private final UserOrganizationMembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public List<Long> listActiveOrganizationIds(Long userId) {
        Instant now = Instant.now();
        Set<Long> organizationIds = new LinkedHashSet<>();
        for (UserOrganizationMembership membership :
                membershipRepository.findActiveByUser_Id(userId, now)) {
            organizationIds.add(membership.getOrganization().getId());
        }
        return List.copyOf(organizationIds);
    }
}
