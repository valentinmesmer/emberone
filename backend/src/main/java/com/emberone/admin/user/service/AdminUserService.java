/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.user.service;

import com.emberone.admin.tenant.domain.Tenant;
import com.emberone.admin.tenant.repository.TenantRepository;
import com.emberone.admin.user.dto.PatchAdminUserAppRoleRequest;
import com.emberone.admin.user.dto.TenantUserListItemDto;
import com.emberone.admin.user.list.AdminUserListDefinition;
import com.emberone.admin.user.list.AdminUserListFilter;
import com.emberone.admin.user.list.AdminUserListSpecs;
import com.emberone.identity.domain.IdentityUser;
import com.emberone.identity.repository.IdentityUserRepository;
import com.emberone.identity.service.IdentityUserService;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.PageDto;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.user.service.UserDeactivationService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Administrative user directory and application-role assignments. */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final IdentityUserRepository identityUserRepository;
    private final TenantRepository tenantRepository;
    private final IdentityUserService identityUserService;
    private final TokenService tokenService;
    private final UserDeactivationService userDeactivationService;

    @Transactional(readOnly = true)
    public PageDto<TenantUserListItemDto> listUsers(ListQuery query, AdminUserListFilter filter) {
        Specification<IdentityUser> scope = AdminUserListSpecs.active();
        if (tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())) {
            // all tenants
        } else if (tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())) {
            Long tenantId = tokenService.getTenantId();
            if (tenantId == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing tenant context");
            }
            for (Long requestedTenantId : filter.tenantIds()) {
                if (!requestedTenantId.equals(tenantId)) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN, "Cannot filter users outside your tenant");
                }
            }
            scope = scope.and(AdminUserListSpecs.inTenants(List.of(tenantId)));
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized");
        }
        Specification<IdentityUser> spec =
                Specification.allOf(
                        scope,
                        AdminUserListSpecs.matching(filter),
                        AdminUserListDefinition.INSTANCE.searchSpecification(query));
        Page<IdentityUser> page =
                identityUserRepository.findAll(
                        spec, AdminUserListDefinition.INSTANCE.pageable(query));
        Map<Long, String> tenantNames =
                tenantRepository
                        .findAllById(
                                page.getContent().stream()
                                        .map(IdentityUser::getTenantId)
                                        .collect(Collectors.toSet()))
                        .stream()
                        .collect(Collectors.toMap(Tenant::getId, Tenant::getName));
        return PageDto.from(
                page,
                page.getContent().stream().map(user -> toListItem(user, tenantNames)).toList());
    }

    /**
     * Single active user of the directory. A user id maps to exactly one identity (and thus one
     * tenant membership). Unknown or deactivated users yield 404; a tenant admin addressing a user
     * of another tenant gets 403. Role changes and deactivation are tenant-scoped by path and
     * answer 404 for users of another tenant.
     */
    @Transactional(readOnly = true)
    public TenantUserListItemDto getUser(long userId) {
        boolean globalAdmin = tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName());
        if (!globalAdmin && !tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized");
        }
        IdentityUser identityUser =
                identityUserRepository
                        .findOne(
                                AdminUserListSpecs.active()
                                        .and(AdminUserListSpecs.withUserId(userId)))
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "User not found"));
        if (!globalAdmin) {
            Long actorTenantId = tokenService.getTenantId();
            if (actorTenantId == null
                    || !Objects.equals(actorTenantId, identityUser.getTenantId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Cannot view users outside your tenant");
            }
        }
        Map<Long, String> tenantNames =
                tenantRepository.findById(identityUser.getTenantId()).stream()
                        .collect(Collectors.toMap(Tenant::getId, Tenant::getName));
        return toListItem(identityUser, tenantNames);
    }

    @Transactional
    public void updateUserAppRole(
            long tenantId, long userId, PatchAdminUserAppRoleRequest request) {
        EmberOneAppRole actorRole = requireActorRole();
        EmberOneAppRole newRole =
                EmberOneAppRole.fromRoleName(request.getAppRole())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "Invalid application role"));
        if (!EmberOneAppRole.actorMayAssignRole(actorRole, newRole)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Cannot assign a role above your own rank");
        }

        IdentityUser identityUser =
                identityUserRepository
                        .findByEmberoneUserId(userId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "User not found"));
        requireInTenant(identityUser, tenantId);
        requireActorMayManageTarget(actorRole, identityUser);
        String previousRole = identityUser.getAppRole();
        identityUserService.updateAppRole(
                identityUser.getTenantId(), userId, newRole.getRoleName());
        log.info(
                "Updated application role for user {}: {} -> {}",
                userId,
                previousRole,
                newRole.getRoleName());
    }

    @Transactional
    public void deactivateUser(long tenantId, long userId) {
        Long actorUserId = tokenService.getUserId();
        if (actorUserId != null && actorUserId.equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Cannot deactivate your own account");
        }

        IdentityUser identityUser =
                identityUserRepository
                        .findByEmberoneUserId(userId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "User not found"));
        requireInTenant(identityUser, tenantId);

        requireActorMayManageTarget(requireActorRole(), identityUser);

        userDeactivationService.deactivateUser(userId);
        log.info("Deactivated user {} via admin API", userId);
    }

    /** Writes are tenant-scoped by path: a user of another tenant is not found. */
    private static void requireInTenant(IdentityUser identityUser, long tenantId) {
        if (!Objects.equals(identityUser.getTenantId(), tenantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    private EmberOneAppRole requireActorRole() {
        return tokenService
                .getAppRole()
                .flatMap(EmberOneAppRole::fromRoleName)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.FORBIDDEN, "Missing application role"));
    }

    private static void requireActorMayManageTarget(
            EmberOneAppRole actorRole, IdentityUser target) {
        EmberOneAppRole targetRole =
                EmberOneAppRole.fromRoleName(target.getAppRole()).orElse(EmberOneAppRole.USER);
        if (!EmberOneAppRole.actorMayAssignRole(actorRole, targetRole)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Cannot manage users above your own rank");
        }
    }

    private TenantUserListItemDto toListItem(
            IdentityUser identityUser, Map<Long, String> tenantNames) {
        return TenantUserListItemDto.builder()
                .userId(identityUser.getEmberoneUserId())
                .tenantId(identityUser.getTenantId())
                .tenantName(tenantNames.getOrDefault(identityUser.getTenantId(), ""))
                .username(identityUser.getUsername())
                .firstName(identityUser.getGivenName())
                .lastName(identityUser.getSurname())
                .appRole(identityUser.getAppRole())
                .build();
    }
}
