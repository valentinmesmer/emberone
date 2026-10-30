/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import com.emberone.management.service.ManagementAccessService;
import com.emberone.organization.cache.OrganizationTenantCache;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.dto.OrganizationGroupDto;
import com.emberone.organizationgroup.service.OrganizationGroupService;
import com.emberone.permission.domain.PermissionType;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationmembership.cache.ActiveOrganizationMembershipCache;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.userpermission.dto.UserPermissionDto;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationAccessService {

    private final TokenService tokenService;
    private final RequestScopedUserPermissions requestScopedUserPermissions;
    private final OrganizationRepository organizationRepository;
    private final OrganizationTenantCache organizationTenantCache;
    private final OrganizationGroupService organizationGroupService;
    private final UserOrganizationGroupMembershipRepository
            userOrganizationGroupMembershipRepository;
    private final UserOrganizationMembershipRepository userOrganizationMembershipRepository;
    private final ActiveOrganizationMembershipCache activeOrganizationMembershipCache;
    private final UserRepository userRepository;
    private final ManagementAccessService managementAccessService;

    public boolean hasOrganizationPermission(Long organizationId, PermissionType permissionType) {
        if (organizationId == null || permissionType == null) return false;

        if (tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())) {
            try {
                return organizationRepository.existsById(organizationId);
            } catch (Exception ignored) {
                return false;
            }
        }

        Long tenantId = tokenService.getTenantId();
        if (tenantId == null || organizationNotInTenant(organizationId, tenantId)) return false;

        if (tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())) return true;

        Map<Long, List<PermissionType>> permissions = loadUserPermissions().getPermissions();
        Collection<PermissionType> orgPermissions =
                permissions == null ? null : permissions.get(organizationId);
        if (orgPermissions != null && orgPermissions.contains(permissionType)) return true;

        return managementAccessService.rightsIn(organizationId).contains(permissionType);
    }

    public boolean isActiveOrganizationMember(Long organizationId) {
        if (organizationId == null) {
            return false;
        }
        Long userId = tokenService.getUserId();
        Long tenantId = tokenService.getTenantId();
        if (userId == null || tenantId == null) {
            return false;
        }
        if (organizationNotInTenant(organizationId, tenantId)) {
            return false;
        }
        return activeOrganizationMembershipCache.loadCached(userId, organizationId).active();
    }

    public boolean hasOrganizationGroupReadPermission(Long organizationGroupId) {
        try {
            OrganizationGroupDto group = organizationGroupService.getGroupById(organizationGroupId);
            return hasOrganizationPermission(
                    group.getOrganizationId(), PermissionType.ORGANIZATION_GROUP_READ);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasUserWritePermissionForMembership(Long membershipId) {
        var membership = userOrganizationGroupMembershipRepository.findById(membershipId);
        if (membership.isEmpty()) {
            return false;
        }
        Long organizationGroupId = membership.get().getOrganizationGroup().getId();
        return hasUserWritePermissionForOrganizationGroup(organizationGroupId);
    }

    public boolean hasUserWritePermissionForOrganizationMembership(Long membershipId) {
        var membership = userOrganizationMembershipRepository.findById(membershipId);
        if (membership.isEmpty()) {
            return false;
        }
        Long organizationId = membership.get().getOrganization().getId();
        return hasOrganizationPermission(organizationId, PermissionType.USER_WRITE);
    }

    public boolean hasUserWritePermissionForOrganizationGroup(Long organizationGroupId) {
        try {
            OrganizationGroupDto group = organizationGroupService.getGroupById(organizationGroupId);
            return hasOrganizationPermission(group.getOrganizationId(), PermissionType.USER_WRITE);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasEventCategoryReadPermission(Long organizationId, String eventCategory) {
        if (eventCategory == null) {
            return false;
        }
        return switch (eventCategory) {
            case "service" ->
                    hasOrganizationPermission(organizationId, PermissionType.SERVICE_EVENT_READ);
            case "operation" ->
                    hasOrganizationPermission(organizationId, PermissionType.OPERATION_EVENT_READ);
            default -> false;
        };
    }

    public boolean canRequestOrganizationMembershipEnd(Long organizationId, Long targetUserId) {
        if (organizationId == null || targetUserId == null) {
            return false;
        }
        Long actorUserId = tokenService.getUserId();
        if (actorUserId == null || actorUserId.equals(targetUserId)) {
            return false;
        }

        var targetUserOptional = userRepository.findById(targetUserId);
        if (targetUserOptional.isEmpty()) {
            return false;
        }
        User targetUser = targetUserOptional.get();
        if (targetUser.getDeactivatedAt() != null) {
            return false;
        }

        Instant now = Instant.now();
        if (userOrganizationMembershipRepository
                .findActiveByUser_IdAndOrganization_Id(targetUserId, organizationId, now)
                .isEmpty()) {
            return false;
        }

        if (tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())) {
            return !organizationNotInTenant(organizationId, targetUser.getTenantId());
        }

        Long actorTenantId = tokenService.getTenantId();
        if (tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())) {
            return actorTenantId != null
                    && actorTenantId.equals(targetUser.getTenantId())
                    && !organizationNotInTenant(organizationId, actorTenantId);
        }

        if (!hasOrganizationPermission(organizationId, PermissionType.USER_WRITE)) {
            return false;
        }
        if (organizationNotInTenant(organizationId, actorTenantId)) {
            return false;
        }

        return true;
    }

    public boolean canRequestMainOrganizationChangeForUser(Long organizationId, Long targetUserId) {
        if (organizationId == null || targetUserId == null) {
            return false;
        }
        Long actorUserId = tokenService.getUserId();
        if (actorUserId == null || actorUserId.equals(targetUserId)) {
            return false;
        }

        var targetUserOptional = userRepository.findById(targetUserId);
        if (targetUserOptional.isEmpty()) {
            return false;
        }
        User targetUser = targetUserOptional.get();
        if (targetUser.getDeactivatedAt() != null) {
            return false;
        }

        Instant now = Instant.now();
        var contextMembership =
                userOrganizationMembershipRepository.findActiveByUser_IdAndOrganization_Id(
                        targetUserId, organizationId, now);
        if (contextMembership.isEmpty()) {
            return false;
        }

        boolean contextIsMain = Boolean.TRUE.equals(contextMembership.get().getMain());
        if (contextIsMain) {
            if (!hasAnotherActiveNonMainMembership(targetUserId, organizationId, now)) {
                return false;
            }
        }

        if (tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())) {
            return !organizationNotInTenant(organizationId, targetUser.getTenantId());
        }

        Long actorTenantId = tokenService.getTenantId();
        if (tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())) {
            return actorTenantId != null
                    && actorTenantId.equals(targetUser.getTenantId())
                    && !organizationNotInTenant(organizationId, actorTenantId);
        }

        if (!hasOrganizationPermission(organizationId, PermissionType.USER_WRITE)) {
            return false;
        }
        return !organizationNotInTenant(organizationId, actorTenantId);
    }

    private boolean hasAnotherActiveNonMainMembership(
            Long userId, Long excludeOrganizationId, Instant now) {
        return userOrganizationMembershipRepository.findActiveByUser_Id(userId, now).stream()
                .anyMatch(
                        m ->
                                !excludeOrganizationId.equals(m.getOrganization().getId())
                                        && !Boolean.TRUE.equals(m.getMain())
                                        && m.getEndDate() == null);
    }

    private UserPermissionDto loadUserPermissions() {
        return requestScopedUserPermissions.getUnfiltered();
    }

    public boolean organizationNotInTenant(Long organizationId, Long tenantId) {
        if (organizationId == null || tenantId == null) {
            return true;
        }
        try {
            return !organizationTenantCache.loadCached(organizationId, tenantId).exists();
        } catch (Exception e) {
            return true;
        }
    }
}
