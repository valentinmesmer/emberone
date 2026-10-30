/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.emberone.admin.tenant.domain.Tenant;
import com.emberone.admin.tenant.repository.TenantRepository;
import com.emberone.admin.user.dto.PatchAdminUserAppRoleRequest;
import com.emberone.admin.user.dto.TenantUserListItemDto;
import com.emberone.admin.user.list.AdminUserListFilter;
import com.emberone.identity.domain.IdentityUser;
import com.emberone.identity.repository.IdentityUserRepository;
import com.emberone.identity.service.IdentityUserService;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.PageDto;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.user.service.UserDeactivationService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock IdentityUserRepository identityUserRepository;
    @Mock TenantRepository tenantRepository;
    @Mock IdentityUserService identityUserService;
    @Mock TokenService tokenService;
    @Mock UserDeactivationService userDeactivationService;
    @InjectMocks AdminUserService service;

    private static final ListQuery QUERY = new ListQuery(0, null, "ada", null);

    @SuppressWarnings("unchecked")
    @Test
    void globalAdminListsPagedUsersAndResolvesTenantNames() {
        IdentityUser user = identityUser(1L, 7L, "ada", EmberOneAppRole.USER);
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(true);
        when(identityUserRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1));
        when(tenantRepository.findAllById(any()))
                .thenReturn(List.of(Tenant.builder().id(7L).name("EmberOne").build()));

        PageDto<TenantUserListItemDto> result = service.listUsers(QUERY, AdminUserListFilter.NONE);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
                .singleElement()
                .satisfies(
                        dto -> {
                            assertThat(dto.getUserId()).isEqualTo(1L);
                            assertThat(dto.getTenantName()).isEqualTo("EmberOne");
                            assertThat(dto.getUsername()).isEqualTo("ada");
                        });
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(identityUserRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("surname")).isNotNull();
        assertThat(pageable.getValue().getSort().getOrderFor("surname").isAscending()).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    void tenantAdminListsOnlyOwnTenantAndRequiresTenantClaim() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())).thenReturn(true);
        when(tokenService.getTenantId()).thenReturn(null);
        assertThatThrownBy(() -> service.listUsers(QUERY, AdminUserListFilter.NONE))
                .isInstanceOf(ResponseStatusException.class);
        verify(identityUserRepository, never())
                .findAll(any(Specification.class), any(Pageable.class));

        IdentityUser user = identityUser(1L, 7L, "ada", EmberOneAppRole.USER);
        when(tokenService.getTenantId()).thenReturn(7L);
        when(identityUserRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1));
        when(tenantRepository.findAllById(any())).thenReturn(List.of());
        assertThat(service.listUsers(QUERY, AdminUserListFilter.NONE).getContent())
                .singleElement()
                .satisfies(dto -> assertThat(dto.getTenantName()).isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    void globalAdminMayFilterByAnyTenantOrganizationAndRole() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(true);
        when(identityUserRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        when(tenantRepository.findAllById(any())).thenReturn(List.of());

        AdminUserListFilter filter =
                AdminUserListFilter.parse(
                        List.of(9L),
                        List.of(3L),
                        List.of(EmberOneAppRole.TENANT_ADMIN.getRoleName()));

        assertThat(service.listUsers(QUERY, filter).getContent()).isEmpty();
        verify(identityUserRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void tenantAdminMayFilterByOwnTenantButNotByForeignTenant() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())).thenReturn(true);
        when(tokenService.getTenantId()).thenReturn(7L);
        when(identityUserRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        when(tenantRepository.findAllById(any())).thenReturn(List.of());

        assertThat(
                        service.listUsers(QUERY, AdminUserListFilter.parse(List.of(7L), null, null))
                                .getContent())
                .isEmpty();

        assertThatThrownBy(
                        () ->
                                service.listUsers(
                                        QUERY, AdminUserListFilter.parse(List.of(8L), null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        ex ->
                                assertThat(((ResponseStatusException) ex).getStatusCode().value())
                                        .isEqualTo(403));
        verify(identityUserRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void appRoleFilterParsesCaseInsensitivelyAndRejectsUnknownValues() {
        assertThat(AdminUserListFilter.parse(null, null, List.of(" tenant_admin ")).appRoles())
                .containsExactly(EmberOneAppRole.TENANT_ADMIN);
        assertThat(AdminUserListFilter.parse(null, null, List.of(" ")).appRoles()).isEmpty();
        assertThatThrownBy(() -> AdminUserListFilter.parse(null, null, List.of("SUPERUSER")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nonAdminCannotListUsers() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())).thenReturn(false);

        assertThatThrownBy(() -> service.listUsers(QUERY, AdminUserListFilter.NONE))
                .isInstanceOf(ResponseStatusException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getUserScopesByRoleAndTenant() {
        IdentityUser user = identityUser(1L, 7L, "ada", EmberOneAppRole.USER);
        when(identityUserRepository.findOne(any(Specification.class)))
                .thenReturn(Optional.of(user));
        when(tenantRepository.findById(7L))
                .thenReturn(Optional.of(Tenant.builder().id(7L).name("EmberOne").build()));

        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(true);
        TenantUserListItemDto dto = service.getUser(1L);
        assertThat(dto.getUserId()).isEqualTo(1L);
        assertThat(dto.getTenantName()).isEqualTo("EmberOne");

        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())).thenReturn(true);
        when(tokenService.getTenantId()).thenReturn(7L);
        assertThat(service.getUser(1L).getUserId()).isEqualTo(1L);

        when(tokenService.getTenantId()).thenReturn(8L);
        assertThatThrownBy(() -> service.getUser(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("outside your tenant");
    }

    @SuppressWarnings("unchecked")
    @Test
    void getUserRejectsNonAdminAndUnknownUser() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())).thenReturn(false);
        assertThatThrownBy(() -> service.getUser(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unauthorized");
        verify(identityUserRepository, never()).findOne(any(Specification.class));

        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(true);
        when(identityUserRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getUser(2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void tenantAdminMayUpdateUserRoleInsideOwnTenant() {
        IdentityUser user = identityUser(1L, 7L, "ada", EmberOneAppRole.USER);
        when(tokenService.getAppRole()).thenReturn(Optional.of("TENANT_ADMIN"));
        when(identityUserRepository.findByEmberoneUserId(1L)).thenReturn(Optional.of(user));

        service.updateUserAppRole(
                7L, 1L, PatchAdminUserAppRoleRequest.builder().appRole("USER").build());

        verify(identityUserService).updateAppRole(7L, 1L, "USER");
    }

    @Test
    void roleUpdateRejectsInvalidElevatedAndCrossTenantChanges() {
        when(tokenService.getAppRole()).thenReturn(Optional.of("TENANT_ADMIN"));
        assertThatThrownBy(
                        () ->
                                service.updateUserAppRole(
                                        7L,
                                        1L,
                                        PatchAdminUserAppRoleRequest.builder()
                                                .appRole("INVALID")
                                                .build()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid");
        assertThatThrownBy(
                        () ->
                                service.updateUserAppRole(
                                        7L,
                                        1L,
                                        PatchAdminUserAppRoleRequest.builder()
                                                .appRole("GLOBAL_ADMIN")
                                                .build()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("above");
        assertThatThrownBy(
                        () ->
                                service.updateUserAppRole(
                                        7L,
                                        1L,
                                        PatchAdminUserAppRoleRequest.builder()
                                                .appRole("ORG_ADMIN")
                                                .build()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid");

        IdentityUser foreign = identityUser(1L, 8L, "ada", EmberOneAppRole.USER);
        when(identityUserRepository.findByEmberoneUserId(1L)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(
                        () ->
                                service.updateUserAppRole(
                                        7L,
                                        1L,
                                        PatchAdminUserAppRoleRequest.builder()
                                                .appRole("USER")
                                                .build()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
        verify(identityUserService, never()).updateAppRole(8L, 1L, "USER");
    }

    @Test
    void tenantAdminMayDeactivateUserInsideOwnTenant() {
        IdentityUser user = identityUser(1L, 7L, "ada", EmberOneAppRole.USER);
        when(tokenService.getUserId()).thenReturn(2L);
        when(tokenService.getAppRole()).thenReturn(Optional.of("TENANT_ADMIN"));
        when(identityUserRepository.findByEmberoneUserId(1L)).thenReturn(Optional.of(user));

        service.deactivateUser(7L, 1L);

        verify(userDeactivationService).deactivateUser(1L);
    }

    @Test
    void deactivateRejectsSelfAndCrossTenant() {
        when(tokenService.getUserId()).thenReturn(1L);
        assertThatThrownBy(() -> service.deactivateUser(7L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("own account");

        IdentityUser foreign = identityUser(1L, 8L, "ada", EmberOneAppRole.USER);
        when(tokenService.getUserId()).thenReturn(2L);
        when(identityUserRepository.findByEmberoneUserId(1L)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.deactivateUser(7L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
        verify(userDeactivationService, never()).deactivateUser(1L);
    }

    @Test
    void tenantAdminMayNotChangeRoleOfGlobalAdmin() {
        IdentityUser globalAdmin = identityUser(1L, 7L, "root", EmberOneAppRole.GLOBAL_ADMIN);
        when(tokenService.getAppRole()).thenReturn(Optional.of("TENANT_ADMIN"));
        when(identityUserRepository.findByEmberoneUserId(1L)).thenReturn(Optional.of(globalAdmin));

        assertThatThrownBy(
                        () ->
                                service.updateUserAppRole(
                                        7L,
                                        1L,
                                        PatchAdminUserAppRoleRequest.builder()
                                                .appRole("USER")
                                                .build()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("above your own rank");
        verify(identityUserService, never()).updateAppRole(7L, 1L, "USER");
    }

    @Test
    void tenantAdminMayNotDeactivateGlobalAdmin() {
        IdentityUser globalAdmin = identityUser(1L, 7L, "root", EmberOneAppRole.GLOBAL_ADMIN);
        when(tokenService.getUserId()).thenReturn(2L);
        when(tokenService.getAppRole()).thenReturn(Optional.of("TENANT_ADMIN"));
        when(identityUserRepository.findByEmberoneUserId(1L)).thenReturn(Optional.of(globalAdmin));

        assertThatThrownBy(() -> service.deactivateUser(7L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("above your own rank");
        verify(userDeactivationService, never()).deactivateUser(1L);
    }

    private IdentityUser identityUser(
            Long userId, Long tenantId, String username, EmberOneAppRole role) {
        return IdentityUser.builder()
                .emberoneUserId(userId)
                .tenantId(tenantId)
                .username(username)
                .givenName("Ada")
                .surname("Lovelace")
                .appRole(role.getRoleName())
                .build();
    }
}
