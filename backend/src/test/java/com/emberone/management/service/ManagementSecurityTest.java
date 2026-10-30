/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emberone.management.domain.ManagementActivation;
import com.emberone.management.domain.ManagementAudience;
import com.emberone.management.domain.ManagementAudienceKind;
import com.emberone.management.repository.ManagementActivationRepository;
import com.emberone.management.repository.ManagementAudienceRepository;
import com.emberone.module.domain.Module;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.security.RequestScopedUserPermissions;
import com.emberone.platform.security.TokenService;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.service.EffectiveStandardResolver;
import com.emberone.standard.service.EnabledModuleResolver;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.userpermission.dto.UserPermissionDto;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Access matrix of the live management check. Covers the rows that do not need a database:
 * activation and time, scope, fail-closed preconditions, tenant boundary, no chains.
 */
@ExtendWith(MockitoExtension.class)
class ManagementSecurityTest {

    private static final long TENANT_ID = 7L;
    private static final long OTHER_TENANT_ID = 8L;
    private static final long USER_ID = 3L;
    private static final long MANAGING_ORG_ID = 10L;
    private static final long TARGET_ORG_ID = 20L;

    @Mock TokenService tokenService;
    @Mock RequestScopedUserPermissions requestScopedUserPermissions;
    @Mock ManagementActivationRepository activationRepository;
    @Mock ManagementAudienceRepository audienceRepository;
    @Mock ManagementCapabilityService capabilityService;
    @Mock OrganizationRepository organizationRepository;
    @Mock EffectiveStandardResolver effectiveStandardResolver;
    @Mock EnabledModuleResolver enabledModuleResolver;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock Standard standard;

    @InjectMocks ManagementAccessService service;

    private Organization managing;
    private Organization target;

    @BeforeEach
    void setUp() {
        managing = org(MANAGING_ORG_ID, TENANT_ID);
        target = org(TARGET_ORG_ID, TENANT_ID);

        lenient().when(tokenService.getTenantId()).thenReturn(TENANT_ID);
        lenient().when(tokenService.getUserId()).thenReturn(USER_ID);
        lenient()
                .when(requestScopedUserPermissions.getUnfiltered())
                .thenReturn(
                        UserPermissionDto.builder()
                                .permissions(
                                        Map.of(
                                                MANAGING_ORG_ID,
                                                List.of(PermissionType.MANAGEMENT_ACCESS)))
                                .build());
        lenient()
                .when(
                        membershipRepository.findActiveByUser_IdAndOrganization_Id(
                                any(), any(), any()))
                .thenReturn(Optional.of(mock(UserOrganizationMembership.class)));
        lenient().when(capabilityService.isCapable(managing)).thenReturn(true);
        lenient()
                .when(organizationRepository.findById(MANAGING_ORG_ID))
                .thenReturn(Optional.of(managing));
        lenient()
                .when(organizationRepository.findById(TARGET_ORG_ID))
                .thenReturn(Optional.of(target));
        lenient()
                .when(organizationRepository.findActiveByTenantIdOrderByName(TENANT_ID))
                .thenReturn(List.of(managing, target));
        lenient()
                .when(effectiveStandardResolver.resolveForOrganization(target))
                .thenReturn(Optional.of(standard));
        lenient()
                .when(enabledModuleResolver.forStandard(standard))
                .thenReturn(Set.of(Module.CORE, Module.APPOINTMENTS));
        lenient()
                .when(
                        activationRepository
                                .findByTenantIdAndUserIdAndEndedAtIsNullAndValidUntilAfterOrderByValidUntilAsc(
                                        eq(TENANT_ID), eq(USER_ID), any()))
                .thenReturn(List.of(activation(TARGET_ORG_ID, "USER_READ,APPOINTMENT_READ")));
        audienceWith(entry(ManagementAudienceKind.ORG, TARGET_ORG_ID));
    }

    @Test
    void grantsChosenScopeWithOpenActivationAndAudienceCoverage() {
        assertThat(service.rightsIn(TARGET_ORG_ID))
                .containsExactlyInAnyOrder(
                        PermissionType.USER_READ, PermissionType.APPOINTMENT_READ);
    }

    @Test
    void scopeOfActivationLimitsRights() {
        when(activationRepository
                        .findByTenantIdAndUserIdAndEndedAtIsNullAndValidUntilAfterOrderByValidUntilAsc(
                                eq(TENANT_ID), eq(USER_ID), any()))
                .thenReturn(List.of(activation(TARGET_ORG_ID, "USER_READ")));

        assertThat(service.rightsIn(TARGET_ORG_ID)).containsExactly(PermissionType.USER_READ);
    }

    @Test
    void noOpenActivationGivesNoRights() {
        when(activationRepository
                        .findByTenantIdAndUserIdAndEndedAtIsNullAndValidUntilAfterOrderByValidUntilAsc(
                                eq(TENANT_ID), eq(USER_ID), any()))
                .thenReturn(List.of());

        assertThat(service.rightsIn(TARGET_ORG_ID)).isEmpty();
    }

    @Test
    void activationForAnotherTargetGivesNoRights() {
        assertThat(service.rightsIn(99L)).isEmpty();
    }

    @Test
    void directManagementAccessIsRequiredLive() {
        when(requestScopedUserPermissions.getUnfiltered())
                .thenReturn(UserPermissionDto.builder().permissions(Map.of()).build());

        assertThat(service.rightsIn(TARGET_ORG_ID)).isEmpty();
    }

    @Test
    void managedRightsDoNotCountAsManagementAccess() {
        when(requestScopedUserPermissions.getUnfiltered())
                .thenReturn(
                        UserPermissionDto.builder()
                                .permissions(
                                        Map.of(MANAGING_ORG_ID, List.of(PermissionType.USER_READ)))
                                .build());

        assertThat(service.rightsIn(TARGET_ORG_ID)).isEmpty();
    }

    @Test
    void capabilityLossGivesNoRights() {
        when(capabilityService.isCapable(managing)).thenReturn(false);

        assertThat(service.rightsIn(TARGET_ORG_ID)).isEmpty();
    }

    @Test
    void endedManagingMembershipGivesNoRights() {
        when(membershipRepository.findActiveByUser_IdAndOrganization_Id(any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThat(service.rightsIn(TARGET_ORG_ID)).isEmpty();
    }

    @Test
    void deactivatedManagingOrTargetGivesNoRights() {
        managing.setDeactivatedAt(Instant.now());
        assertThat(service.rightsIn(TARGET_ORG_ID)).isEmpty();
    }

    @Test
    void deactivatedTargetGivesNoRights() {
        target.setDeactivatedAt(Instant.now());
        assertThat(service.rightsIn(TARGET_ORG_ID)).isEmpty();
    }

    @Test
    void removedAudienceEntryGivesNoRights() {
        audienceWith();

        assertThat(service.rightsIn(TARGET_ORG_ID)).isEmpty();
    }

    @Test
    void tenantAudienceCoversAnyTargetOfTheTenant() {
        audienceWith(entry(ManagementAudienceKind.TENANT, null));

        assertThat(service.rightsIn(TARGET_ORG_ID))
                .containsExactlyInAnyOrder(
                        PermissionType.USER_READ, PermissionType.APPOINTMENT_READ);
    }

    @Test
    void rightsOfModulesTheTargetDoesNotHaveAreDropped() {
        when(enabledModuleResolver.forStandard(standard)).thenReturn(Set.of(Module.CORE));

        assertThat(service.rightsIn(TARGET_ORG_ID)).containsExactly(PermissionType.USER_READ);
    }

    @Test
    void managementAccessIsNeverGranted() {
        when(activationRepository
                        .findByTenantIdAndUserIdAndEndedAtIsNullAndValidUntilAfterOrderByValidUntilAsc(
                                eq(TENANT_ID), eq(USER_ID), any()))
                .thenReturn(List.of(activation(TARGET_ORG_ID, "MANAGEMENT_ACCESS,USER_READ")));

        assertThat(service.rightsIn(TARGET_ORG_ID)).containsExactly(PermissionType.USER_READ);
    }

    @Test
    void otherTenantsTargetGivesNoRights() {
        Organization foreign = org(TARGET_ORG_ID, OTHER_TENANT_ID);
        when(organizationRepository.findById(TARGET_ORG_ID)).thenReturn(Optional.of(foreign));

        assertThat(service.rightsIn(TARGET_ORG_ID)).isEmpty();
    }

    @Test
    void missingTenantOrUserGivesNoRights() {
        when(tokenService.getTenantId()).thenReturn(null);
        assertThat(service.rightsIn(TARGET_ORG_ID)).isEmpty();
    }

    @Test
    void danglingOrganizationReferenceGivesNoRights() {
        audienceWith(entry(ManagementAudienceKind.ORG, 999L));

        assertThat(service.rightsIn(TARGET_ORG_ID)).isEmpty();
    }

    @Test
    void failsClosedOnException() {
        when(organizationRepository.findById(TARGET_ORG_ID))
                .thenThrow(new IllegalStateException("db down"));

        assertThat(service.rightsIn(TARGET_ORG_ID)).isEmpty();
    }

    private void audienceWith(ManagementAudience... entries) {
        lenient()
                .when(
                        audienceRepository.findByManagingOrganizationIdOrderByIdAsc(
                                eq(MANAGING_ORG_ID)))
                .thenReturn(List.of(entries));
    }

    private static Organization org(long id, long tenantId) {
        return Organization.builder().id(id).tenantId(tenantId).name("Org " + id).build();
    }

    /** Every type an audience entry may allow: all except management access. */
    private static final EnumSet<PermissionType> ALLOWED_TYPES = allowedTypes();

    private static EnumSet<PermissionType> allowedTypes() {
        EnumSet<PermissionType> types = EnumSet.allOf(PermissionType.class);
        types.remove(PermissionType.MANAGEMENT_ACCESS);
        return types;
    }

    private static ManagementAudience entry(ManagementAudienceKind kind, Long refId) {
        return ManagementAudience.builder()
                .tenantId(TENANT_ID)
                .managingOrganizationId(MANAGING_ORG_ID)
                .kind(kind)
                .refId(refId)
                .permissionTypes(ManagementActivationService.encode(ALLOWED_TYPES))
                .maxDurationMinutes(120)
                .build();
    }

    private static ManagementActivation activation(long targetId, String permissionTypes) {
        return ManagementActivation.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .userId(USER_ID)
                .managingOrganizationId(MANAGING_ORG_ID)
                .targetOrganizationId(targetId)
                .activatedAt(Instant.now().minusSeconds(60))
                .validUntil(Instant.now().plusSeconds(3600))
                .permissionTypes(permissionTypes)
                .reason("Einsatzunterstützung im Landkreis")
                .build();
    }
}
