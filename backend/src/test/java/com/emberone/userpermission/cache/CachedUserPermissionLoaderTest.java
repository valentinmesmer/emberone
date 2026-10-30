/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userpermission.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.emberone.organization.domain.Organization;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.permission.domain.PermissionGroupType;
import com.emberone.permission.domain.PermissionType;
import com.emberone.permissiongroup.domain.PermissionGroup;
import com.emberone.permissiongroupmapping.domain.PermissionGroupMapping;
import com.emberone.permissiongroupmapping.repository.PermissionGroupMappingRepository;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CachedUserPermissionLoaderTest {

    private static final Long USER_ID = 1L;
    private static final Long TENANT_ID = 2L;
    private static final Long FIRST_ORG_ID = 10L;
    private static final Long SECOND_ORG_ID = 11L;

    @Mock private UserOrganizationGroupMembershipRepository membershipRepository;

    @Mock private UserOrganizationMembershipRepository organizationMembershipRepository;

    @Mock private PermissionGroupMappingRepository mappingRepository;

    @InjectMocks private CachedUserPermissionLoader loader;

    private static UserOrganizationGroupMembership membership(
            OrganizationGroup group, Instant startDate, Instant endDate) {
        return UserOrganizationGroupMembership.builder()
                .organizationGroup(group)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private static UserOrganizationMembership orgMembership(
            Long organizationId, Instant startDate, Instant endDate) {
        return UserOrganizationMembership.builder()
                .organization(Organization.builder().id(organizationId).build())
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private static OrganizationGroup group(Long id, Long organizationId) {
        return OrganizationGroup.builder()
                .id(id)
                .organization(Organization.builder().id(organizationId).build())
                .build();
    }

    private static PermissionGroup permissionGroup(
            Long id, Long organizationId, PermissionType... types) {
        PermissionGroup group =
                PermissionGroup.builder()
                        .id(id)
                        .tenantId(TENANT_ID)
                        .organizationId(organizationId)
                        .type(PermissionGroupType.CUSTOM)
                        .build();
        group.setPermissionTypes(java.util.Arrays.stream(types).toList());
        return group;
    }

    private static PermissionGroupMapping mapping(PermissionGroup permissionGroup) {
        return PermissionGroupMapping.builder().permissionGroup(permissionGroup).build();
    }

    @Test
    void load_returnsEmptyPermissionsWhenNoMembershipsExist() {
        when(membershipRepository.findByUserId(USER_ID)).thenReturn(List.of());

        var result = loader.load(TENANT_ID, USER_ID);

        assertThat(result.getPermissions()).isEmpty();
        assertThat(result.getTenantLocale()).isNull();
        verify(mappingRepository, never()).findByOrganizationGroupIdIn(anySet());
    }

    @Test
    void load_ignoresFutureAndExpiredMemberships() {
        Instant now = Instant.now();
        OrganizationGroup futureGroup = group(100L, FIRST_ORG_ID);
        OrganizationGroup expiredGroup = group(101L, FIRST_ORG_ID);
        when(membershipRepository.findByUserId(USER_ID))
                .thenReturn(
                        List.of(
                                membership(futureGroup, now.plus(1, ChronoUnit.DAYS), null),
                                membership(
                                        expiredGroup,
                                        now.minus(10, ChronoUnit.DAYS),
                                        now.minus(1, ChronoUnit.DAYS))));

        var result = loader.load(TENANT_ID, USER_ID);

        assertThat(result.getPermissions()).isEmpty();
        verify(mappingRepository, never()).findByOrganizationGroupIdIn(anySet());
    }

    @Test
    void load_ignoresActiveGroupMembershipWhenOrganizationMembershipExpired() {
        Instant now = Instant.now();
        OrganizationGroup group = group(100L, FIRST_ORG_ID);
        when(membershipRepository.findByUserId(USER_ID))
                .thenReturn(List.of(membership(group, now.minus(1, ChronoUnit.DAYS), null)));
        when(organizationMembershipRepository.findActiveByUser_Id(eq(USER_ID), any(Instant.class)))
                .thenReturn(List.of());

        var result = loader.load(TENANT_ID, USER_ID);

        assertThat(result.getPermissions()).isEmpty();
        verify(mappingRepository, never()).findByOrganizationGroupIdIn(anySet());
    }

    @Test
    void load_includesMembershipsOnStartAndEndBoundary() {
        Instant now = Instant.now();
        OrganizationGroup startsToday = group(100L, FIRST_ORG_ID);
        OrganizationGroup endsToday = group(101L, FIRST_ORG_ID);
        when(membershipRepository.findByUserId(USER_ID))
                .thenReturn(
                        List.of(
                                membership(startsToday, now.minus(1, ChronoUnit.SECONDS), null),
                                membership(
                                        endsToday,
                                        now.minus(5, ChronoUnit.DAYS),
                                        now.plus(1, ChronoUnit.SECONDS))));
        when(organizationMembershipRepository.findActiveByUser_Id(eq(USER_ID), any(Instant.class)))
                .thenReturn(
                        List.of(orgMembership(FIRST_ORG_ID, now.minus(1, ChronoUnit.DAYS), null)));
        when(mappingRepository.findByOrganizationGroupIdIn(Set.of(100L, 101L)))
                .thenReturn(List.of());

        loader.load(TENANT_ID, USER_ID);

        verify(mappingRepository).findByOrganizationGroupIdIn(Set.of(100L, 101L));
    }

    @Test
    void load_mergesDeduplicatesAndSortsPermissionsByOrganization() {
        Instant now = Instant.now();
        OrganizationGroup firstMembershipGroup = group(100L, FIRST_ORG_ID);
        OrganizationGroup secondMembershipGroup = group(101L, SECOND_ORG_ID);
        PermissionGroup firstPermissionGroup =
                permissionGroup(
                        200L,
                        FIRST_ORG_ID,
                        PermissionType.USER_WRITE,
                        PermissionType.ORGANIZATION_READ);
        PermissionGroup overlappingPermissionGroup =
                permissionGroup(
                        201L,
                        FIRST_ORG_ID,
                        PermissionType.USER_WRITE,
                        PermissionType.APPOINTMENT_READ);
        PermissionGroup secondOrganizationGroup =
                permissionGroup(202L, SECOND_ORG_ID, PermissionType.DOCUMENT_READ);
        when(membershipRepository.findByUserId(USER_ID))
                .thenReturn(
                        List.of(
                                membership(
                                        firstMembershipGroup, now.minus(1, ChronoUnit.DAYS), null),
                                membership(
                                        secondMembershipGroup,
                                        now.minus(1, ChronoUnit.DAYS),
                                        now.plus(1, ChronoUnit.DAYS))));
        when(organizationMembershipRepository.findActiveByUser_Id(eq(USER_ID), any(Instant.class)))
                .thenReturn(
                        List.of(
                                orgMembership(FIRST_ORG_ID, now.minus(1, ChronoUnit.DAYS), null),
                                orgMembership(SECOND_ORG_ID, now.minus(1, ChronoUnit.DAYS), null)));
        when(mappingRepository.findByOrganizationGroupIdIn(Set.of(100L, 101L)))
                .thenReturn(
                        List.of(
                                mapping(firstPermissionGroup), mapping(overlappingPermissionGroup),
                                mapping(firstPermissionGroup), mapping(secondOrganizationGroup)));

        var result = loader.load(TENANT_ID, USER_ID);

        assertThat(result.getPermissions()).containsOnlyKeys(FIRST_ORG_ID, SECOND_ORG_ID);
        assertThat(result.getPermissions().get(FIRST_ORG_ID))
                .containsExactly(
                        PermissionType.ORGANIZATION_READ,
                        PermissionType.USER_WRITE,
                        PermissionType.APPOINTMENT_READ);
        assertThat(result.getPermissions().get(SECOND_ORG_ID))
                .containsExactly(PermissionType.DOCUMENT_READ);
        assertThat(result.getTenantLocale()).isNull();
    }

    @Test
    void load_includesPermissionsForAllOrganizationsWhenMapped() {
        Instant now = Instant.now();
        OrganizationGroup membershipGroup = group(100L, FIRST_ORG_ID);
        PermissionGroup included = permissionGroup(200L, FIRST_ORG_ID, PermissionType.USER_READ);
        PermissionGroup excluded = permissionGroup(201L, SECOND_ORG_ID, PermissionType.USER_WRITE);
        when(membershipRepository.findByUserId(USER_ID))
                .thenReturn(
                        List.of(membership(membershipGroup, now.minus(1, ChronoUnit.DAYS), null)));
        when(organizationMembershipRepository.findActiveByUser_Id(eq(USER_ID), any(Instant.class)))
                .thenReturn(
                        List.of(orgMembership(FIRST_ORG_ID, now.minus(1, ChronoUnit.DAYS), null)));
        when(mappingRepository.findByOrganizationGroupIdIn(Set.of(100L)))
                .thenReturn(List.of(mapping(included), mapping(excluded)));

        var result = loader.load(TENANT_ID, USER_ID);

        assertThat(result.getPermissions()).containsOnlyKeys(FIRST_ORG_ID, SECOND_ORG_ID);
    }

    @Test
    void load_returnsEmptyMapWhenActiveGroupsHaveNoMappings() {
        Instant now = Instant.now();
        OrganizationGroup membershipGroup = group(100L, FIRST_ORG_ID);
        when(membershipRepository.findByUserId(USER_ID))
                .thenReturn(
                        List.of(membership(membershipGroup, now.minus(1, ChronoUnit.DAYS), null)));
        when(organizationMembershipRepository.findActiveByUser_Id(eq(USER_ID), any(Instant.class)))
                .thenReturn(
                        List.of(orgMembership(FIRST_ORG_ID, now.minus(1, ChronoUnit.DAYS), null)));
        when(mappingRepository.findByOrganizationGroupIdIn(Set.of(100L))).thenReturn(List.of());

        assertThat(loader.load(TENANT_ID, USER_ID).getPermissions()).isEmpty();
    }
}
