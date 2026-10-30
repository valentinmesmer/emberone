/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.service;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.mapper.OrganizationMapper;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.organizationgroup.mapper.OrganizationGroupMapper;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.security.OrganizationAccessService;
import com.emberone.user.dto.GroupMembershipSectionDto;
import com.emberone.user.dto.OrganizationMembershipSectionDto;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.dto.UserOrganizationGroupMembershipDto;
import com.emberone.userorganizationgroupmembership.mapper.UserOrganizationGroupMembershipMapper;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.dto.UserOrganizationMembershipDto;
import com.emberone.userorganizationmembership.mapper.UserOrganizationMembershipMapper;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.organizationmembershipend.OrganizationMembershipEndRequestWorkflowOpenAccess;
import com.emberone.workflow.organizationusermainorganizationchange.OrganizationUserMainOrganizationChangeWorkflowOpenAccess;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserMembershipOverviewService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final UserOrganizationMembershipRepository organizationMembershipRepository;
    private final UserOrganizationGroupMembershipRepository groupMembershipRepository;
    private final UserOrganizationMembershipMapper organizationMembershipMapper;
    private final UserOrganizationGroupMembershipMapper groupMembershipMapper;
    private final OrganizationMapper organizationMapper;
    private final OrganizationGroupMapper organizationGroupMapper;
    private final OrganizationAccessService organizationAccessService;
    private final OrganizationMembershipEndRequestWorkflowOpenAccess
            organizationMembershipEndRequestWorkflowOpenAccess;
    private final OrganizationUserMainOrganizationChangeWorkflowOpenAccess
            organizationUserMainOrganizationChangeWorkflowOpenAccess;

    @Transactional(readOnly = true)
    public OrganizationMembershipSectionDto getMembershipOverview(
            Long organizationId, Long userId) {
        Organization organization =
                organizationRepository
                        .findById(organizationId)
                        .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        List<UserOrganizationMembership> organizationMemberships =
                organizationMembershipRepository.findByUser_IdAndOrganization_Id(
                        userId, organizationId);
        List<UserOrganizationGroupMembership> groupMemberships =
                groupMembershipRepository.findByUser_IdAndOrganizationGroup_Organization_Id(
                        userId, organizationId);

        boolean canWrite =
                organizationAccessService.hasOrganizationPermission(
                        organizationId, PermissionType.USER_WRITE);
        boolean canRequestOrganizationMembershipEnd =
                canWrite
                        && organizationMembershipEndRequestWorkflowOpenAccess.canOpen(
                                organizationId, userId);
        boolean canRequestOrganizationUserMainOrganizationChange =
                canWrite
                        && organizationUserMainOrganizationChangeWorkflowOpenAccess.canOpen(
                                organizationId, userId);
        Instant now = Instant.now();
        boolean inLeadershipGroup =
                groupMemberships.stream()
                        .anyMatch(
                                membership ->
                                        membership.getOrganizationGroup().getType()
                                                        == OrganizationGroupType.LEADERSHIP
                                                && membership.isActiveAt(now));

        List<UserOrganizationMembershipDto> membershipDtos =
                organizationMemberships.stream()
                        .map(organizationMembershipMapper::toDto)
                        .sorted(organizationMembershipRowComparator())
                        .toList();

        return OrganizationMembershipSectionDto.builder()
                .organization(organizationMapper.toDto(organization))
                .canWrite(canWrite)
                .inLeadershipGroup(inLeadershipGroup)
                .canRequestOrganizationMembershipEnd(canRequestOrganizationMembershipEnd)
                .canRequestOrganizationUserMainOrganizationChange(
                        canRequestOrganizationUserMainOrganizationChange)
                .organizationMemberships(membershipDtos)
                .groups(buildGroupSections(groupMemberships))
                .build();
    }

    private List<GroupMembershipSectionDto> buildGroupSections(
            List<UserOrganizationGroupMembership> groupMemberships) {
        Map<Long, OrganizationGroup> groupsById = new LinkedHashMap<>();
        Map<Long, List<UserOrganizationGroupMembership>> membershipsByGroup = new LinkedHashMap<>();

        for (UserOrganizationGroupMembership membership : groupMemberships) {
            OrganizationGroup group = membership.getOrganizationGroup();
            if (!isGroupVisibleInOverview(group)) {
                continue;
            }
            groupsById.putIfAbsent(group.getId(), group);
            membershipsByGroup
                    .computeIfAbsent(group.getId(), ignored -> new ArrayList<>())
                    .add(membership);
        }

        return groupsById.values().stream()
                .sorted(
                        Comparator.comparing(
                                        OrganizationGroup::getName, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(OrganizationGroup::getId))
                .map(
                        group -> {
                            List<UserOrganizationGroupMembershipDto> membershipDtos =
                                    membershipsByGroup
                                            .getOrDefault(group.getId(), List.of())
                                            .stream()
                                            .map(groupMembershipMapper::toDto)
                                            .sorted(groupMembershipRowComparator())
                                            .toList();
                            return GroupMembershipSectionDto.builder()
                                    .group(organizationGroupMapper.toDto(group))
                                    .memberships(membershipDtos)
                                    .build();
                        })
                .filter(section -> !section.getMemberships().isEmpty())
                .toList();
    }

    private static boolean isGroupVisibleInOverview(OrganizationGroup group) {
        if (group.getType() == OrganizationGroupType.ALL_MEMBERS) {
            return false;
        }
        return group.getType().isVisibleInUserInterface() || group.isArchived();
    }

    private static Comparator<UserOrganizationMembershipDto> organizationMembershipRowComparator() {
        return Comparator.comparing(UserOrganizationMembershipDto::getStartDate)
                .thenComparing(UserOrganizationMembershipDto::getId);
    }

    private static Comparator<UserOrganizationGroupMembershipDto> groupMembershipRowComparator() {
        return Comparator.comparing(UserOrganizationGroupMembershipDto::getStartDate)
                .thenComparing(UserOrganizationGroupMembershipDto::getId);
    }
}
