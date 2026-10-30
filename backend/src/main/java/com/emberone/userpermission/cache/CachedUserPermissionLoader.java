/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userpermission.cache;

import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.permission.domain.PermissionType;
import com.emberone.permissiongroup.domain.PermissionGroup;
import com.emberone.permissiongroupmapping.domain.PermissionGroupMapping;
import com.emberone.permissiongroupmapping.repository.PermissionGroupMappingRepository;
import com.emberone.platform.cache.CacheNames;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.userpermission.dto.UserPermissionDto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CachedUserPermissionLoader {

    private final UserOrganizationGroupMembershipRepository membershipRepository;
    private final UserOrganizationMembershipRepository organizationMembershipRepository;
    private final PermissionGroupMappingRepository mappingRepository;

    private static UserPermissionDto emptyPermissions() {
        return UserPermissionDto.builder().permissions(Map.of()).build();
    }

    /**
     * Cached permission map only. Tenant locale is resolved separately so locale changes do not
     * require user-permission cache eviction.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.USER_PERMISSIONS, key = "#tenantId + ':' + #userId")
    public UserPermissionDto load(Long tenantId, Long userId) {
        log.debug("Loading permissions from database for user {}", userId);

        List<UserOrganizationGroupMembership> memberships =
                membershipRepository.findByUserId(userId);
        if (memberships.isEmpty()) {
            return emptyPermissions();
        }

        Instant now = Instant.now();
        List<UserOrganizationGroupMembership> activeMemberships =
                memberships.stream().filter(membership -> membership.isActiveAt(now)).toList();

        if (activeMemberships.isEmpty()) {
            return emptyPermissions();
        }

        Set<Long> activeOrganizationIds =
                organizationMembershipRepository.findActiveByUser_Id(userId, now).stream()
                        .map(membership -> membership.getOrganization().getId())
                        .collect(Collectors.toSet());

        activeMemberships =
                activeMemberships.stream()
                        .filter(
                                membership ->
                                        activeOrganizationIds.contains(
                                                membership
                                                        .getOrganizationGroup()
                                                        .getOrganization()
                                                        .getId()))
                        .toList();

        if (activeMemberships.isEmpty()) {
            return emptyPermissions();
        }

        List<PermissionGroupMapping> mappings =
                mappingRepository.findByOrganizationGroupIdIn(
                        activeMemberships.stream()
                                .map(UserOrganizationGroupMembership::getOrganizationGroup)
                                .map(OrganizationGroup::getId)
                                .collect(Collectors.toSet()));

        Map<Long, List<PermissionType>> permissionsByOrg =
                mappings.stream()
                        .map(PermissionGroupMapping::getPermissionGroup)
                        .collect(
                                Collectors.groupingBy(
                                        PermissionGroup::getOrganizationId,
                                        Collectors.flatMapping(
                                                group -> group.getPermissionTypes().stream(),
                                                Collectors.collectingAndThen(
                                                        Collectors.toSet(),
                                                        set -> set.stream().sorted().toList()))));

        return UserPermissionDto.builder().permissions(permissionsByOrg).build();
    }
}
