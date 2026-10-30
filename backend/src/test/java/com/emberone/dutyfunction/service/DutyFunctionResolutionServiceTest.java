/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.dutyfunction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.dutyfunction.standard.FunctionGrant;
import com.emberone.dutyfunction.standard.FunctionGrantRepository;
import com.emberone.dutyfunction.tenant.TenantFunction;
import com.emberone.dutyfunction.tenant.TenantFunctionRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.domain.GroupMembershipType;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.rank.repository.UserRankRepository;
import com.emberone.rank.tenant.TenantRankRepository;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.service.EffectiveStandardResolver;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DutyFunctionResolutionServiceTest {

    private static final long TENANT_ID = 1L;
    private static final long USER_ID = 10L;
    private static final long ORG_ID = 100L;
    private static final long STANDARD_ID = 50L;

    @Mock OrganizationRepository organizationRepository;
    @Mock OrganizationGroupRepository organizationGroupRepository;
    @Mock UserOrganizationGroupMembershipRepository groupMembershipRepository;
    @Mock UserOrganizationMembershipRepository organizationMembershipRepository;
    @Mock EffectiveStandardResolver effectiveStandardResolver;
    @Mock FunctionGrantRepository grantRepository;
    @Mock TenantFunctionRepository functionRepository;
    @Mock UserRankRepository userRankRepository;
    @Mock TenantRankRepository tenantRankRepository;

    @InjectMocks DutyFunctionResolutionService service;

    @BeforeEach
    void bindTenant() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void leadershipLeaderResolvesToKommandant() {
        Organization organization = Organization.builder().id(ORG_ID).tenantId(TENANT_ID).build();
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
        Standard standard = Standard.builder().id(STANDARD_ID).tenantId(TENANT_ID).build();
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.of(standard));
        when(grantRepository.findByStandardId(STANDARD_ID))
                .thenReturn(
                        List.of(
                                FunctionGrant.builder()
                                        .organizationGroupConfigKey(
                                                OrganizationGroupType.LEADERSHIP.name())
                                        .membershipType(GroupMembershipType.LEADER)
                                        .functionConfigKey("KDT")
                                        .build()));
        OrganizationGroup leadershipGroup =
                OrganizationGroup.builder()
                        .name("Führung")
                        .configKey(OrganizationGroupType.LEADERSHIP.name())
                        .organization(organization)
                        .type(OrganizationGroupType.LEADERSHIP)
                        .build();
        UserOrganizationGroupMembership membership =
                UserOrganizationGroupMembership.builder()
                        .organizationGroup(leadershipGroup)
                        .membershipType(GroupMembershipType.LEADER)
                        .startDate(Instant.parse("2025-01-01T00:00:00Z"))
                        .build();
        when(groupMembershipRepository.findByUser_IdAndOrganizationGroup_Organization_Id(
                        USER_ID, ORG_ID))
                .thenReturn(List.of(membership));
        when(functionRepository.findByConfigKey("KDT"))
                .thenReturn(
                        Optional.of(
                                TenantFunction.builder()
                                        .configKey("KDT")
                                        .name("Kommandant")
                                        .abbreviation("KDT")
                                        .active(true)
                                        .archived(false)
                                        .build()));

        var resolved = service.resolveForUserInOrganization(USER_ID, ORG_ID);
        assertThat(resolved).hasSize(1);
        assertThat(resolved.getFirst().getFunctionName()).isEqualTo("Kommandant");
        assertThat(resolved.getFirst().getMembershipType()).isEqualTo(GroupMembershipType.LEADER);
        assertThat(resolved.getFirst().isActive()).isTrue();
    }

    @Test
    void endedLeadershipMembershipResolvesAsInactiveHistory() {
        Organization organization = Organization.builder().id(ORG_ID).tenantId(TENANT_ID).build();
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
        Standard standard = Standard.builder().id(STANDARD_ID).tenantId(TENANT_ID).build();
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.of(standard));
        when(grantRepository.findByStandardId(STANDARD_ID))
                .thenReturn(
                        List.of(
                                FunctionGrant.builder()
                                        .organizationGroupConfigKey(
                                                OrganizationGroupType.LEADERSHIP.name())
                                        .membershipType(GroupMembershipType.LEADER)
                                        .functionConfigKey("KDT")
                                        .build()));
        OrganizationGroup leadershipGroup =
                OrganizationGroup.builder()
                        .name("Führung")
                        .configKey(OrganizationGroupType.LEADERSHIP.name())
                        .organization(organization)
                        .type(OrganizationGroupType.LEADERSHIP)
                        .build();
        Instant end = Instant.parse("2025-06-01T00:00:00Z");
        UserOrganizationGroupMembership ended =
                UserOrganizationGroupMembership.builder()
                        .organizationGroup(leadershipGroup)
                        .membershipType(GroupMembershipType.LEADER)
                        .startDate(Instant.parse("2020-01-01T00:00:00Z"))
                        .endDate(end)
                        .build();
        UserOrganizationGroupMembership notStarted =
                UserOrganizationGroupMembership.builder()
                        .organizationGroup(leadershipGroup)
                        .membershipType(GroupMembershipType.LEADER)
                        .startDate(Instant.now().plusSeconds(86_400))
                        .build();
        when(groupMembershipRepository.findByUser_IdAndOrganizationGroup_Organization_Id(
                        USER_ID, ORG_ID))
                .thenReturn(List.of(ended, notStarted));
        when(functionRepository.findByConfigKey("KDT"))
                .thenReturn(
                        Optional.of(
                                TenantFunction.builder()
                                        .configKey("KDT")
                                        .name("Kommandant")
                                        .abbreviation("KDT")
                                        .active(true)
                                        .archived(false)
                                        .build()));

        var resolved = service.resolveForUserInOrganization(USER_ID, ORG_ID);
        assertThat(resolved).hasSize(1);
        assertThat(resolved.getFirst().isActive()).isFalse();
        assertThat(resolved.getFirst().getMembershipEndDate()).isEqualTo(end);
    }

    @Test
    void leadershipMemberHasNoFunctionWhenOnlyRoleGrantsExist() {
        Organization organization = Organization.builder().id(ORG_ID).tenantId(TENANT_ID).build();
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
        Standard standard = Standard.builder().id(STANDARD_ID).tenantId(TENANT_ID).build();
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.of(standard));
        when(grantRepository.findByStandardId(STANDARD_ID))
                .thenReturn(
                        List.of(
                                FunctionGrant.builder()
                                        .organizationGroupConfigKey(
                                                OrganizationGroupType.LEADERSHIP.name())
                                        .membershipType(GroupMembershipType.LEADER)
                                        .functionConfigKey("KDT")
                                        .build()));
        OrganizationGroup leadershipGroup =
                OrganizationGroup.builder()
                        .configKey(OrganizationGroupType.LEADERSHIP.name())
                        .organization(organization)
                        .build();
        UserOrganizationGroupMembership membership =
                UserOrganizationGroupMembership.builder()
                        .organizationGroup(leadershipGroup)
                        .membershipType(GroupMembershipType.MEMBER)
                        .startDate(Instant.now())
                        .build();
        when(groupMembershipRepository.findByUser_IdAndOrganizationGroup_Organization_Id(
                        USER_ID, ORG_ID))
                .thenReturn(List.of(membership));

        assertThat(service.resolveForUserInOrganization(USER_ID, ORG_ID)).isEmpty();
    }
}
