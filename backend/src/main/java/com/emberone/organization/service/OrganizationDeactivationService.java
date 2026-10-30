/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.service;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.dto.DeactivateOrganizationRequest;
import com.emberone.organization.exception.OrganizationDeactivatedException;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.user.service.UserDeactivationService;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.userorganizationmembership.service.UserOrganizationMembershipService;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationDeactivationService {

    private final OrganizationRepository organizationRepository;
    private final UserOrganizationMembershipRepository membershipRepository;
    private final UserOrganizationMembershipService membershipService;
    private final UserDeactivationService userDeactivationService;

    @Transactional
    public void deactivateOrganization(
            long tenantId, long organizationId, DeactivateOrganizationRequest request) {
        Organization organization =
                organizationRepository
                        .findById(organizationId)
                        .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        if (!organization.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Organization does not belong to this tenant");
        }
        if (organization.isDeactivated()) {
            throw new OrganizationDeactivatedException(organizationId);
        }

        String confirmation = request.getConfirmationName().trim();
        if (!organization.getName().equals(confirmation)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Confirmation does not match organization name");
        }

        List<Organization> tenantOrganizations = organizationRepository.findAllWithParentFetch();
        Set<Long> subtreeIds =
                OrganizationSubtreeSupport.collectSubtreeIds(organizationId, tenantOrganizations);
        Instant now = Instant.now();

        for (Organization org : tenantOrganizations) {
            if (subtreeIds.contains(org.getId()) && org.isDeactivated()) {
                throw new OrganizationDeactivatedException(org.getId());
            }
        }

        List<UserOrganizationMembership> activeMemberships =
                membershipRepository.findActiveByOrganization_IdIn(subtreeIds, now);

        Set<Long> deactivatedUserIds = new HashSet<>();
        for (UserOrganizationMembership membership : activeMemberships) {
            if (!Boolean.TRUE.equals(membership.getMain())) {
                continue;
            }
            long userId = membership.getUser().getId();
            if (deactivatedUserIds.add(userId)) {
                userDeactivationService.deactivateUser(userId);
            }
        }

        for (UserOrganizationMembership membership : activeMemberships) {
            long userId = membership.getUser().getId();
            if (deactivatedUserIds.contains(userId)) {
                continue;
            }
            membershipService.endActiveMembershipForUserDeactivation(membership, now);
        }

        for (Organization org : tenantOrganizations) {
            if (subtreeIds.contains(org.getId())) {
                org.setDeactivatedAt(now);
            }
        }
        organizationRepository.saveAll(
                tenantOrganizations.stream()
                        .filter(org -> subtreeIds.contains(org.getId()))
                        .toList());

        log.info(
                "Deactivated organization subtree rootId={} tenantId={} orgCount={}",
                organizationId,
                tenantId,
                subtreeIds.size());
    }

    public static void requireActive(Organization organization) {
        if (organization.isDeactivated()) {
            throw new OrganizationDeactivatedException(organization.getId());
        }
    }
}
