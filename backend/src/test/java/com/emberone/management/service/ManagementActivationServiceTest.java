/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.management.domain.ManagementActivation;
import com.emberone.management.domain.ManagementActivationEndedBy;
import com.emberone.management.domain.ManagementAudience;
import com.emberone.management.domain.ManagementAudienceKind;
import com.emberone.management.dto.ManagementActivationDto;
import com.emberone.management.dto.ManagementActivationRequest;
import com.emberone.management.exception.ManagementActivationNotFoundException;
import com.emberone.management.exception.ManagementActivationValidationException;
import com.emberone.management.repository.ManagementActivationRepository;
import com.emberone.management.repository.ManagementAudienceRepository;
import com.emberone.module.domain.Module;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.permission.domain.PermissionType;
import com.emberone.permission.realtime.PermissionRealtimeNotifier;
import com.emberone.platform.exception.AccessDeniedException;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.service.EffectiveStandardResolver;
import com.emberone.standard.service.EnabledModuleResolver;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.userpermission.dto.UserPermissionDto;
import com.emberone.userpermission.service.UserPermissionService;
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

@ExtendWith(MockitoExtension.class)
class ManagementActivationServiceTest {

    private static final long TENANT_ID = 7L;
    private static final long USER_ID = 3L;
    private static final long MANAGING_ORG_ID = 10L;
    private static final long TARGET_ORG_ID = 20L;
    private static final String REASON = "Einsatzunterstützung im Landkreis";

    @Mock ManagementActivationRepository activationRepository;
    @Mock ManagementAudienceRepository audienceRepository;
    @Mock ManagementCapabilityService capabilityService;
    @Mock OrganizationRepository organizationRepository;
    @Mock EffectiveStandardResolver effectiveStandardResolver;
    @Mock EnabledModuleResolver enabledModuleResolver;
    @Mock UserPermissionService userPermissionService;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock PermissionRealtimeNotifier permissionRealtimeNotifier;
    @Mock Standard standard;

    @InjectMocks ManagementActivationService service;

    private Organization managing;
    private Organization target;

    @BeforeEach
    void setUp() {
        managing =
                Organization.builder()
                        .id(MANAGING_ORG_ID)
                        .tenantId(TENANT_ID)
                        .name("Geschaeftsstelle")
                        .build();
        target =
                Organization.builder()
                        .id(TARGET_ORG_ID)
                        .tenantId(TENANT_ID)
                        .name("Zielwache")
                        .build();
        lenient()
                .when(organizationRepository.findById(MANAGING_ORG_ID))
                .thenReturn(Optional.of(managing));
        lenient()
                .when(organizationRepository.findById(TARGET_ORG_ID))
                .thenReturn(Optional.of(target));
        lenient()
                .when(organizationRepository.findActiveByTenantIdOrderByName(TENANT_ID))
                .thenReturn(List.of(managing, target));
        lenient().when(capabilityService.isCapable(managing)).thenReturn(true);
        lenient()
                .when(userPermissionService.getUserPermissions(USER_ID))
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
        lenient()
                .when(effectiveStandardResolver.resolveForOrganization(target))
                .thenReturn(Optional.of(standard));
        lenient().when(enabledModuleResolver.forStandard(standard)).thenReturn(Set.of(Module.CORE));
        lenient()
                .when(activationRepository.saveAndFlush(any(ManagementActivation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void activatesWithinOneEntryAndRealtimeNotifies() {
        audienceWith(entry(ManagementAudienceKind.ORG, TARGET_ORG_ID, 120));

        ManagementActivationDto dto = service.activate(TENANT_ID, USER_ID, request(60, REASON));

        assertThat(dto.getTargetOrganizationId()).isEqualTo(TARGET_ORG_ID);
        assertThat(dto.getPermissionTypes()).containsExactly(PermissionType.USER_READ);
        verify(permissionRealtimeNotifier).notifyPermissionsChanged(TENANT_ID, USER_ID);
    }

    @Test
    void rejectsReasonOutsideLength() {
        assertThatThrownBy(() -> service.activate(TENANT_ID, USER_ID, request(60, "   ")))
                .isInstanceOf(ManagementActivationValidationException.class);
        verify(activationRepository, never()).save(any());
    }

    @Test
    void rejectsDurationAboveEntryMaximum() {
        audienceWith(entry(ManagementAudienceKind.ORG, TARGET_ORG_ID, 30));

        assertThatThrownBy(() -> service.activate(TENANT_ID, USER_ID, request(60, REASON)))
                .isInstanceOf(ManagementActivationValidationException.class);
        verify(activationRepository, never()).save(any());
    }

    @Test
    void rejectsWithoutDirectManagementAccess() {
        when(userPermissionService.getUserPermissions(USER_ID))
                .thenReturn(UserPermissionDto.builder().permissions(Map.of()).build());

        assertThatThrownBy(() -> service.activate(TENANT_ID, USER_ID, request(60, REASON)))
                .isInstanceOf(AccessDeniedException.class);
        verify(activationRepository, never()).save(any());
    }

    @Test
    void reactivationEndsThePreviousOpenActivation() {
        ManagementActivation previous =
                ManagementActivation.builder()
                        .id(99L)
                        .tenantId(TENANT_ID)
                        .userId(USER_ID)
                        .targetOrganizationId(TARGET_ORG_ID)
                        .build();
        when(activationRepository.findByUserIdAndTargetOrganizationIdAndEndedAtIsNull(
                        USER_ID, TARGET_ORG_ID))
                .thenReturn(Optional.of(previous));
        audienceWith(entry(ManagementAudienceKind.ORG, TARGET_ORG_ID, 120));

        service.activate(TENANT_ID, USER_ID, request(60, REASON));

        assertThat(previous.getEndedBy()).isEqualTo(ManagementActivationEndedBy.USER);
        assertThat(previous.getEndedAt()).isNotNull();
    }

    @Test
    void endOwnRejectsActivationOfAnotherUser() {
        when(activationRepository.findByIdAndUserIdAndEndedAtIsNull(5L, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.endOwn(TENANT_ID, USER_ID, 5L))
                .isInstanceOf(ManagementActivationNotFoundException.class);
    }

    @Test
    void systemEndEndsOpenActivationsAndReturnsUsers() {
        ManagementActivation open =
                ManagementActivation.builder().id(1L).tenantId(TENANT_ID).userId(USER_ID).build();
        when(activationRepository.findByManagingOrganizationIdAndEndedAtIsNull(MANAGING_ORG_ID))
                .thenReturn(List.of(open));

        Set<Long> users = service.endSystemActivationsThrough(MANAGING_ORG_ID);

        assertThat(users).containsExactly(USER_ID);
        assertThat(open.getEndedBy()).isEqualTo(ManagementActivationEndedBy.SYSTEM);
    }

    private void audienceWith(ManagementAudience... entries) {
        when(audienceRepository.findByManagingOrganizationIdOrderByIdAsc(MANAGING_ORG_ID))
                .thenReturn(List.of(entries));
    }

    private static ManagementAudience entry(
            ManagementAudienceKind kind, Long refId, int maxDuration) {
        return ManagementAudience.builder()
                .tenantId(TENANT_ID)
                .managingOrganizationId(MANAGING_ORG_ID)
                .kind(kind)
                .refId(refId)
                .permissionTypes(
                        ManagementActivationService.encode(
                                EnumSet.complementOf(EnumSet.of(PermissionType.MANAGEMENT_ACCESS))))
                .maxDurationMinutes(maxDuration)
                .build();
    }

    private static ManagementActivationRequest request(int minutes, String reason) {
        ManagementActivationRequest request = new ManagementActivationRequest();
        request.setManagingOrganizationId(MANAGING_ORG_ID);
        request.setTargetOrganizationId(TARGET_ORG_ID);
        request.setDurationMinutes(minutes);
        request.setPermissionTypes(List.of(PermissionType.USER_READ));
        request.setReason(reason);
        return request;
    }
}
