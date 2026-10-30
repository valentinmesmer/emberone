/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.emberone.appointment.domain.Appointment;
import com.emberone.appointment.domain.AudienceScope;
import com.emberone.appointment.exception.AppointmentAccessDeniedException;
import com.emberone.appointment.repository.AppointmentRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.security.OrganizationAccessService;
import com.emberone.user.domain.User;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentAudienceServiceTest {

    @Mock UserOrganizationMembershipRepository organizationMembershipRepository;
    @Mock UserOrganizationGroupMembershipRepository groupMembershipRepository;
    @Mock AppointmentRepository appointmentRepository;
    @Mock OrganizationAccessService organizationAccessService;

    @Test
    void allAudienceRequiresAppointmentReadPermission() {
        AppointmentAudienceService service = service();
        Appointment appointment = appointment(AudienceScope.ALL);
        when(organizationAccessService.hasOrganizationPermission(
                        10L, PermissionType.APPOINTMENT_READ))
                .thenReturn(false, true);

        assertThat(service.canUserAccessAppointment(1L, appointment)).isFalse();
        assertThat(service.canUserAccessAppointment(1L, appointment)).isTrue();
    }

    @Test
    void selectedAudienceRequiresActiveMatchingGroup() {
        AppointmentAudienceService service = service();
        Appointment appointment = appointment(AudienceScope.SELECTED_GROUPS);
        appointment.setAudienceGroupIds(List.of(20L));
        when(organizationAccessService.hasOrganizationPermission(
                        10L, PermissionType.APPOINTMENT_READ))
                .thenReturn(true);
        when(groupMembershipRepository.findActiveGroupIdsForUserInOrganization(
                        org.mockito.ArgumentMatchers.eq(1L),
                        org.mockito.ArgumentMatchers.eq(10L),
                        anyInstant()))
                .thenReturn(List.of(21L))
                .thenReturn(List.of(20L));

        assertThat(service.canUserAccessAppointment(1L, appointment)).isFalse();
        assertThat(service.canUserAccessAppointment(1L, appointment)).isTrue();
    }

    @Test
    void ensureAccessHidesMissingAndInvisibleAppointmentsBehindAccessException() {
        AppointmentAudienceService service = service();
        when(appointmentRepository.findById(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.ensureUserCanAccess(1L, 5L))
                .isInstanceOf(AppointmentAccessDeniedException.class);

        Appointment appointment = appointment(AudienceScope.ALL);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        when(organizationAccessService.hasOrganizationPermission(
                        10L, PermissionType.APPOINTMENT_READ))
                .thenReturn(false);
        assertThatThrownBy(() -> service.ensureUserCanAccess(1L, 5L))
                .isInstanceOf(AppointmentAccessDeniedException.class);
    }

    @Test
    void filterAppointmentsVisibleToUserCachesPermissionAndGroupLookups() {
        AppointmentAudienceService service = service();
        Organization orgA = Organization.builder().id(10L).build();
        Organization orgB = Organization.builder().id(20L).build();
        Appointment allAudience =
                Appointment.builder()
                        .id(1L)
                        .organization(orgA)
                        .audienceScope(AudienceScope.ALL)
                        .build();
        Appointment selectedAudience =
                Appointment.builder()
                        .id(2L)
                        .organization(orgA)
                        .audienceScope(AudienceScope.SELECTED_GROUPS)
                        .build();
        selectedAudience.setAudienceGroupIds(List.of(30L));
        Appointment otherOrg =
                Appointment.builder()
                        .id(3L)
                        .organization(orgB)
                        .audienceScope(AudienceScope.ALL)
                        .build();
        when(organizationAccessService.hasOrganizationPermission(
                        10L, PermissionType.APPOINTMENT_READ))
                .thenReturn(true);
        when(organizationAccessService.hasOrganizationPermission(
                        20L, PermissionType.APPOINTMENT_READ))
                .thenReturn(false);
        when(groupMembershipRepository.findActiveGroupIdsForUserInOrganization(
                        org.mockito.ArgumentMatchers.eq(1L),
                        org.mockito.ArgumentMatchers.eq(10L),
                        anyInstant()))
                .thenReturn(List.of(30L));

        assertThat(
                        service.filterAppointmentsVisibleToUser(
                                1L, List.of(allAudience, selectedAudience, otherOrg)))
                .containsExactly(allAudience, selectedAudience);
        org.mockito.Mockito.verify(organizationAccessService, org.mockito.Mockito.times(1))
                .hasOrganizationPermission(10L, PermissionType.APPOINTMENT_READ);
        org.mockito.Mockito.verify(groupMembershipRepository, org.mockito.Mockito.times(1))
                .findActiveGroupIdsForUserInOrganization(
                        org.mockito.ArgumentMatchers.eq(1L),
                        org.mockito.ArgumentMatchers.eq(10L),
                        anyInstant());
    }

    @Test
    void selectedAudienceRejectsEmptyOrMissingGroupIds() {
        AppointmentAudienceService service = service();
        Appointment emptyGroups = appointment(AudienceScope.SELECTED_GROUPS);
        emptyGroups.setAudienceGroupIds(List.of());
        when(organizationAccessService.hasOrganizationPermission(
                        10L, PermissionType.APPOINTMENT_READ))
                .thenReturn(true);
        assertThat(service.canUserAccessAppointment(1L, emptyGroups)).isFalse();

        Appointment nullGroups = appointment(AudienceScope.SELECTED_GROUPS);
        nullGroups.setAudienceGroupIds(null);
        assertThat(service.canUserAccessAppointment(1L, nullGroups)).isFalse();
    }

    @Test
    void filterAppointmentsVisibleToUserReturnsEmptyForNullOrEmptyInput() {
        AppointmentAudienceService service = service();
        assertThat(service.filterAppointmentsVisibleToUser(1L, null)).isEmpty();
        assertThat(service.filterAppointmentsVisibleToUser(1L, List.of())).isEmpty();
    }

    @Test
    void resolvesAllOrganizationMembersOrSelectedGroupMembers() {
        AppointmentAudienceService service = service();
        Appointment appointment = appointment(AudienceScope.ALL);
        when(organizationMembershipRepository.findActiveByOrganization_Id(
                        eq(10L), any(Instant.class)))
                .thenReturn(
                        List.of(
                                UserOrganizationMembership.builder()
                                        .user(User.builder().id(1L).build())
                                        .startDate(Instant.now())
                                        .build(),
                                UserOrganizationMembership.builder()
                                        .user(User.builder().id(2L).build())
                                        .startDate(Instant.now())
                                        .build()));
        assertThat(service.resolveAudienceUserIds(appointment)).containsExactlyInAnyOrder(1L, 2L);

        appointment.setAudienceScope(AudienceScope.SELECTED_GROUPS);
        appointment.setAudienceGroupIds(List.of(20L));
        when(groupMembershipRepository.findActiveUserIdsByGroupIds(
                        org.mockito.ArgumentMatchers.eq(List.of(20L)), anyInstant()))
                .thenReturn(List.of(2L, 3L));
        assertThat(service.resolveAudienceUserIds(appointment)).containsExactlyInAnyOrder(2L, 3L);
    }

    private AppointmentAudienceService service() {
        return new AppointmentAudienceService(
                organizationMembershipRepository, groupMembershipRepository,
                appointmentRepository, organizationAccessService);
    }

    private Appointment appointment(AudienceScope scope) {
        return Appointment.builder()
                .id(5L)
                .organization(Organization.builder().id(10L).build())
                .audienceScope(scope)
                .build();
    }

    private Instant anyInstant() {
        return org.mockito.ArgumentMatchers.any(Instant.class);
    }
}
