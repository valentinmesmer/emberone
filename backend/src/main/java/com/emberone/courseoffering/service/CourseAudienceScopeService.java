/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves the audience scope of a user against the current organization tree. */
@Service
@RequiredArgsConstructor
public class CourseAudienceScopeService {

    private final UserOrganizationMembershipRepository membershipRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public CourseAudienceScope forUser(Long userId) {
        Map<Long, Organization> organizations = new HashMap<>();
        for (Organization organization : organizationRepository.findAllWithParentFetch()) {
            organizations.put(organization.getId(), organization);
        }
        Set<Long> memberIds = new HashSet<>();
        Set<Long> ancestorOrSelfIds = new HashSet<>();
        Set<Long> typeIds = new HashSet<>();
        Map<Long, Organization> visited = new HashMap<>();
        for (UserOrganizationMembership membership :
                membershipRepository.findActiveByUser_Id(userId, Instant.now())) {
            Organization member = organizations.get(membership.getOrganization().getId());
            if (member == null) {
                continue;
            }
            memberIds.add(member.getId());
            typeIds.add(member.getType().getId());
            for (Organization current = member;
                    current != null && ancestorOrSelfIds.add(current.getId());
                    current = current.getParentOrganization()) {
                // walks up to the root; the set guards against cycles
                visited.put(current.getId(), current);
            }
        }
        Map<Long, Integer> depths = new HashMap<>();
        visited.forEach((id, organization) -> depths.put(id, depth(organization)));
        return new CourseAudienceScope(memberIds, ancestorOrSelfIds, typeIds, depths);
    }

    /** Number of ancestors above the organization (root = 0); guards against cycles. */
    private static int depth(Organization organization) {
        Set<Long> seen = new HashSet<>();
        int depth = 0;
        for (Organization current = organization.getParentOrganization();
                current != null && seen.add(current.getId());
                current = current.getParentOrganization()) {
            depth++;
        }
        return depth;
    }
}
