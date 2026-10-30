/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.service;

import com.emberone.organizationgroup.domain.GroupMembershipType;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.organizationgroup.exception.SystemGroupModificationException;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.permission.event.UserPermissionsChangedEvent;
import com.emberone.user.domain.User;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllMembersGroupService {

    private final OrganizationGroupRepository organizationGroupRepository;
    private final UserRepository userRepository;
    private final UserOrganizationGroupMembershipRepository groupMembershipRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void ensureUserInAllMembersGroup(Long userId, Long organizationId) {
        OrganizationGroup allMembersGroup =
                organizationGroupRepository
                        .findByOrganizationIdAndType(
                                organizationId, OrganizationGroupType.ALL_MEMBERS)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "AllMembers group not found for organization: "
                                                        + organizationId));

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));

        Instant now = Instant.now();
        List<UserOrganizationGroupMembership> existingMemberships =
                groupMembershipRepository.findByUser_IdAndOrganizationGroup_Id(
                        userId, allMembersGroup.getId());

        boolean alreadyMember =
                existingMemberships.stream().anyMatch(membership -> membership.isActiveAt(now));

        if (alreadyMember) {
            return;
        }

        UserOrganizationGroupMembership membership =
                UserOrganizationGroupMembership.builder()
                        .tenantId(user.getTenantId())
                        .user(user)
                        .organizationGroup(allMembersGroup)
                        .startDate(now)
                        .membershipType(GroupMembershipType.MEMBER)
                        .build();

        groupMembershipRepository.save(membership);
        eventPublisher.publishEvent(new UserPermissionsChangedEvent(user.getTenantId(), userId));
        log.info(
                "Added user {} to AllMembers group {} in organization {}",
                userId,
                allMembersGroup.getId(),
                organizationId);
    }

    public boolean isAllMembersGroup(OrganizationGroup organizationGroup) {
        return organizationGroup.getType() == OrganizationGroupType.ALL_MEMBERS;
    }

    public boolean isLeadershipGroup(OrganizationGroup organizationGroup) {
        return organizationGroup.getType() == OrganizationGroupType.LEADERSHIP;
    }

    /**
     * Whether the user currently has an active membership in the organization's leadership group.
     * Leadership members cannot leave the org or the leadership group except via tenant admin.
     */
    public boolean hasActiveLeadershipMembership(Long userId, Long organizationId) {
        return groupMembershipRepository.existsActiveLeadershipMembership(
                userId, organizationId, Instant.now());
    }

    public void validateMembershipModificationAllowed(OrganizationGroup organizationGroup) {
        if (isAllMembersGroup(organizationGroup)) {
            throw new SystemGroupModificationException(
                    "Cannot manually modify memberships in the AllMembers organization group");
        }
        if (isLeadershipGroup(organizationGroup)) {
            throw new SystemGroupModificationException(
                    "Cannot manually modify memberships in the Leadership organization group");
        }
    }
}
