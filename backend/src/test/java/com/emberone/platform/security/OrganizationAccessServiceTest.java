/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.emberone.management.service.ManagementAccessService;
import com.emberone.organization.cache.OrganizationTenantCache;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.dto.OrganizationGroupDto;
import com.emberone.organizationgroup.service.OrganizationGroupService;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.cache.value.ActiveOrganizationMembershipCacheValue;
import com.emberone.platform.cache.value.OrganizationInTenantCacheValue;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationmembership.cache.ActiveOrganizationMembershipCache;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.userpermission.dto.UserPermissionDto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationAccessServiceTest {

    private static final Long ORG_ID = 10L;
    private static final Long TENANT_ID = 5L;

    @Mock private TokenService tokenService;

    @Mock private RequestScopedUserPermissions requestScopedUserPermissions;

    @Mock private OrganizationRepository organizationRepository;

    @Mock private OrganizationTenantCache organizationTenantCache;

    @Mock private OrganizationGroupService organizationGroupService;

    @Mock
    private UserOrganizationGroupMembershipRepository userOrganizationGroupMembershipRepository;

    @Mock private UserOrganizationMembershipRepository userOrganizationMembershipRepository;

    @Mock private ActiveOrganizationMembershipCache activeOrganizationMembershipCache;

    @Mock private UserRepository userRepository;

    @Mock private ManagementAccessService managementAccessService;

    @InjectMocks private OrganizationAccessService organizationAccessService;

    @Test
    void hasOrganizationPermission_returnsFalseWhenIdsOrPermissionMissing() {
        assertThat(
                        organizationAccessService.hasOrganizationPermission(
                                null, PermissionType.USER_READ))
                .isFalse();
        assertThat(organizationAccessService.hasOrganizationPermission(ORG_ID, null)).isFalse();
    }

    @Test
    void hasOrganizationPermission_returnsFalseWhenTenantMissingOrOrgNotInTenant() {
        when(tokenService.getTenantId()).thenReturn(null);

        assertThat(
                        organizationAccessService.hasOrganizationPermission(
                                ORG_ID, PermissionType.USER_READ))
                .isFalse();

        when(tokenService.getTenantId()).thenReturn(TENANT_ID);
        when(organizationTenantCache.loadCached(ORG_ID, TENANT_ID))
                .thenReturn(new OrganizationInTenantCacheValue(false));

        assertThat(
                        organizationAccessService.hasOrganizationPermission(
                                ORG_ID, PermissionType.USER_READ))
                .isFalse();
    }

    @Test
    void hasOrganizationPermission_grantsTenantAdminInOwnTenant() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(tokenService.getTenantId()).thenReturn(TENANT_ID);
        when(organizationTenantCache.loadCached(ORG_ID, TENANT_ID))
                .thenReturn(new OrganizationInTenantCacheValue(true));
        when(tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())).thenReturn(true);

        assertThat(
                        organizationAccessService.hasOrganizationPermission(
                                ORG_ID, PermissionType.APPOINTMENT_WRITE))
                .isTrue();
    }

    @Test
    void hasOrganizationPermission_grantsGlobalAdminForExistingOrganization() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(true);
        when(organizationRepository.existsById(ORG_ID)).thenReturn(true);

        assertThat(
                        organizationAccessService.hasOrganizationPermission(
                                ORG_ID, PermissionType.ORGANIZATION_READ))
                .isTrue();
    }

    @Test
    void hasOrganizationPermission_checksUserPermissionsForRegularUser() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(tokenService.getTenantId()).thenReturn(TENANT_ID);
        when(organizationTenantCache.loadCached(ORG_ID, TENANT_ID))
                .thenReturn(new OrganizationInTenantCacheValue(true));
        when(tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())).thenReturn(false);
        when(requestScopedUserPermissions.getUnfiltered())
                .thenReturn(
                        UserPermissionDto.builder()
                                .permissions(Map.of(ORG_ID, List.of(PermissionType.USER_READ)))
                                .build());

        assertThat(
                        organizationAccessService.hasOrganizationPermission(
                                ORG_ID, PermissionType.USER_READ))
                .isTrue();
        assertThat(
                        organizationAccessService.hasOrganizationPermission(
                                ORG_ID, PermissionType.USER_WRITE))
                .isFalse();
    }

    @Test
    void hasOrganizationPermission_fallsBackToManagementRightsWithoutDirectGrant() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(tokenService.getTenantId()).thenReturn(TENANT_ID);
        when(organizationTenantCache.loadCached(ORG_ID, TENANT_ID))
                .thenReturn(new OrganizationInTenantCacheValue(true));
        when(tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())).thenReturn(false);
        when(requestScopedUserPermissions.getUnfiltered())
                .thenReturn(UserPermissionDto.builder().permissions(Map.of()).build());
        when(managementAccessService.rightsIn(ORG_ID))
                .thenReturn(java.util.Set.of(PermissionType.USER_READ));

        assertThat(
                        organizationAccessService.hasOrganizationPermission(
                                ORG_ID, PermissionType.USER_READ))
                .isTrue();
        assertThat(
                        organizationAccessService.hasOrganizationPermission(
                                ORG_ID, PermissionType.USER_WRITE))
                .isFalse();
    }

    @Test
    void hasOrganizationPermission_returnsFalseWhenPermissionMapMissing() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(tokenService.getTenantId()).thenReturn(TENANT_ID);
        when(organizationTenantCache.loadCached(ORG_ID, TENANT_ID))
                .thenReturn(new OrganizationInTenantCacheValue(true));
        when(tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())).thenReturn(false);
        when(requestScopedUserPermissions.getUnfiltered())
                .thenReturn(UserPermissionDto.builder().build());

        assertThat(
                        organizationAccessService.hasOrganizationPermission(
                                ORG_ID, PermissionType.USER_READ))
                .isFalse();
    }

    @Test
    void organizationNotInTenant_returnsTrueWhenIdsMissing() {
        assertThat(organizationAccessService.organizationNotInTenant(null, TENANT_ID)).isTrue();
        assertThat(organizationAccessService.organizationNotInTenant(ORG_ID, null)).isTrue();
    }

    @Test
    void organizationNotInTenant_reflectsRepositoryLookup() {
        when(organizationTenantCache.loadCached(ORG_ID, TENANT_ID))
                .thenReturn(new OrganizationInTenantCacheValue(true));
        assertThat(organizationAccessService.organizationNotInTenant(ORG_ID, TENANT_ID)).isFalse();

        when(organizationTenantCache.loadCached(ORG_ID, TENANT_ID))
                .thenReturn(new OrganizationInTenantCacheValue(false));
        assertThat(organizationAccessService.organizationNotInTenant(ORG_ID, TENANT_ID)).isTrue();
    }

    @Test
    void organizationNotInTenant_treatsRepositoryErrorsAsOutOfTenant() {
        when(organizationTenantCache.loadCached(ORG_ID, TENANT_ID))
                .thenThrow(new RuntimeException("db down"));

        assertThat(organizationAccessService.organizationNotInTenant(ORG_ID, TENANT_ID)).isTrue();
    }

    @Test
    void isActiveOrganizationMember_requiresUserTenantAndActiveMembership() {
        when(tokenService.getUserId()).thenReturn(null);
        when(tokenService.getTenantId()).thenReturn(TENANT_ID);
        assertThat(organizationAccessService.isActiveOrganizationMember(ORG_ID)).isFalse();

        when(tokenService.getUserId()).thenReturn(5L);
        when(tokenService.getTenantId()).thenReturn(null);
        assertThat(organizationAccessService.isActiveOrganizationMember(ORG_ID)).isFalse();

        when(tokenService.getTenantId()).thenReturn(TENANT_ID);
        when(organizationTenantCache.loadCached(ORG_ID, TENANT_ID))
                .thenReturn(new OrganizationInTenantCacheValue(false));
        assertThat(organizationAccessService.isActiveOrganizationMember(ORG_ID)).isFalse();

        when(organizationTenantCache.loadCached(ORG_ID, TENANT_ID))
                .thenReturn(new OrganizationInTenantCacheValue(true));
        when(activeOrganizationMembershipCache.loadCached(5L, ORG_ID))
                .thenReturn(new ActiveOrganizationMembershipCacheValue(false));
        assertThat(organizationAccessService.isActiveOrganizationMember(ORG_ID)).isFalse();

        when(activeOrganizationMembershipCache.loadCached(5L, ORG_ID))
                .thenReturn(new ActiveOrganizationMembershipCacheValue(true));
        assertThat(organizationAccessService.isActiveOrganizationMember(ORG_ID)).isTrue();
    }

    @Test
    void hasEventCategoryReadPermission_mapsKnownCategories() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(true);
        when(organizationRepository.existsById(ORG_ID)).thenReturn(true);

        assertThat(organizationAccessService.hasEventCategoryReadPermission(ORG_ID, null))
                .isFalse();
        assertThat(organizationAccessService.hasEventCategoryReadPermission(ORG_ID, "service"))
                .isTrue();
        assertThat(organizationAccessService.hasEventCategoryReadPermission(ORG_ID, "unknown"))
                .isFalse();
    }

    @Test
    void hasUserWritePermissionForMembership_returnsFalseWhenMembershipMissing() {
        when(userOrganizationGroupMembershipRepository.findById(1L)).thenReturn(Optional.empty());

        assertThat(organizationAccessService.hasUserWritePermissionForMembership(1L)).isFalse();
    }

    @Test
    void hasUserWritePermissionForMembership_delegatesViaOrganizationGroup() {
        OrganizationGroupDto group = OrganizationGroupDto.builder().organizationId(ORG_ID).build();
        UserOrganizationGroupMembership membership =
                UserOrganizationGroupMembership.builder()
                        .organizationGroup(OrganizationGroup.builder().id(7L).build())
                        .build();

        when(userOrganizationGroupMembershipRepository.findById(1L))
                .thenReturn(Optional.of(membership));
        when(organizationGroupService.getGroupById(7L)).thenReturn(group);
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(true);
        when(organizationRepository.existsById(ORG_ID)).thenReturn(true);

        assertThat(organizationAccessService.hasUserWritePermissionForMembership(1L)).isTrue();
    }

    @Test
    void organizationGroupPermissionsResolveOwningOrganization() {
        when(organizationGroupService.getGroupById(7L))
                .thenReturn(OrganizationGroupDto.builder().organizationId(ORG_ID).build());
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(true);
        when(organizationRepository.existsById(ORG_ID)).thenReturn(true);

        assertThat(organizationAccessService.hasOrganizationGroupReadPermission(7L)).isTrue();
    }

    @Test
    void organizationGroupReadFailsClosedOnLookupError() {
        when(organizationGroupService.getGroupById(9L))
                .thenThrow(new RuntimeException("not found"));
        assertThat(organizationAccessService.hasOrganizationGroupReadPermission(9L)).isFalse();
    }

    @Test
    void canRequestOrganizationMembershipEnd_rejectsSelfAndDeactivatedTarget() {
        when(tokenService.getUserId()).thenReturn(42L);
        assertThat(organizationAccessService.canRequestOrganizationMembershipEnd(ORG_ID, 42L))
                .isFalse();

        User deactivated =
                User.builder()
                        .id(99L)
                        .tenantId(TENANT_ID)
                        .personalNumber(2L)
                        .firstName("A")
                        .lastName("B")
                        .username("ab")
                        .deactivatedAt(Instant.now())
                        .build();
        when(tokenService.getUserId()).thenReturn(5L);
        when(userRepository.findById(99L)).thenReturn(Optional.of(deactivated));
        assertThat(organizationAccessService.canRequestOrganizationMembershipEnd(ORG_ID, 99L))
                .isFalse();
    }

    @Test
    void canRequestOrganizationMembershipEnd_allowsActiveMembershipIncludingMain() {
        User target =
                User.builder()
                        .id(99L)
                        .tenantId(TENANT_ID)
                        .personalNumber(2L)
                        .firstName("A")
                        .lastName("B")
                        .username("ab")
                        .build();
        when(tokenService.getUserId()).thenReturn(5L);
        when(userRepository.findById(99L)).thenReturn(Optional.of(target));
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())).thenReturn(false);
        when(tokenService.getTenantId()).thenReturn(TENANT_ID);
        when(organizationTenantCache.loadCached(ORG_ID, TENANT_ID))
                .thenReturn(new OrganizationInTenantCacheValue(true));
        when(requestScopedUserPermissions.getUnfiltered())
                .thenReturn(
                        UserPermissionDto.builder()
                                .permissions(Map.of(ORG_ID, List.of(PermissionType.USER_WRITE)))
                                .build());

        when(userOrganizationMembershipRepository.findActiveByUser_IdAndOrganization_Id(
                        eq(99L), eq(ORG_ID), any(Instant.class)))
                .thenReturn(Optional.empty());
        assertThat(organizationAccessService.canRequestOrganizationMembershipEnd(ORG_ID, 99L))
                .isFalse();

        Organization org = Organization.builder().id(ORG_ID).tenantId(TENANT_ID).build();
        when(userOrganizationMembershipRepository.findActiveByUser_IdAndOrganization_Id(
                        eq(99L), eq(ORG_ID), any(Instant.class)))
                .thenReturn(
                        Optional.of(
                                UserOrganizationMembership.builder()
                                        .organization(org)
                                        .main(true)
                                        .build()));

        assertThat(organizationAccessService.canRequestOrganizationMembershipEnd(ORG_ID, 99L))
                .isTrue();
    }
}
