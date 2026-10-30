/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import com.emberone.appointment.domain.Appointment;
import com.emberone.appointment.domain.AudienceScope;
import com.emberone.appointment.exception.AppointmentAccessDeniedException;
import com.emberone.appointment.repository.AppointmentRepository;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.security.OrganizationAccessService;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentAudienceService {

    private final UserOrganizationMembershipRepository userOrganizationMembershipRepository;
    private final UserOrganizationGroupMembershipRepository groupMembershipRepository;
    private final AppointmentRepository appointmentRepository;
    private final OrganizationAccessService organizationAccessService;

    public boolean canUserAccessAppointment(Long userId, Appointment appointment) {
        Instant now = Instant.now();
        Map<Long, Boolean> readOkByOrg = new HashMap<>();
        Map<Long, Set<Long>> userGroupIdsByOrg = new HashMap<>();
        return isAppointmentVisibleToUser(userId, appointment, now, readOkByOrg, userGroupIdsByOrg);
    }

    public List<Appointment> filterAppointmentsVisibleToUser(
            Long userId, List<Appointment> appointments) {
        if (appointments == null || appointments.isEmpty()) {
            return List.of();
        }
        Instant now = Instant.now();
        Map<Long, Boolean> readOkByOrg = new HashMap<>();
        Map<Long, Set<Long>> userGroupIdsByOrg = new HashMap<>();
        List<Appointment> visible = new ArrayList<>(appointments.size());
        for (Appointment appointment : appointments) {
            if (isAppointmentVisibleToUser(
                    userId, appointment, now, readOkByOrg, userGroupIdsByOrg)) {
                visible.add(appointment);
            }
        }
        return visible;
    }

    private boolean isAppointmentVisibleToUser(
            Long userId,
            Appointment appointment,
            Instant at,
            Map<Long, Boolean> readOkByOrg,
            Map<Long, Set<Long>> userGroupIdsByOrg) {
        Long organizationId = appointment.getOrganization().getId();
        if (!readOkByOrg.computeIfAbsent(
                organizationId,
                id ->
                        organizationAccessService.hasOrganizationPermission(
                                id, PermissionType.APPOINTMENT_READ))) {
            return false;
        }
        if (appointment.getAudienceScope() == AudienceScope.ALL) {
            return true;
        }
        var audienceGroupIds = appointment.getAudienceGroupIds();
        if (audienceGroupIds == null || audienceGroupIds.isEmpty()) {
            return false;
        }
        Set<Long> userGroupIds =
                userGroupIdsByOrg.computeIfAbsent(
                        organizationId,
                        id ->
                                new HashSet<>(
                                        groupMembershipRepository
                                                .findActiveGroupIdsForUserInOrganization(
                                                        userId, id, at)));
        return userGroupIds.stream().anyMatch(audienceGroupIds::contains);
    }

    public void ensureUserCanAccess(Long userId, Long appointmentId) {
        Appointment appointment =
                appointmentRepository
                        .findById(appointmentId)
                        .orElseThrow(
                                () ->
                                        new AppointmentAccessDeniedException(
                                                "Appointment not found"));
        if (!canUserAccessAppointment(userId, appointment)) {
            throw new AppointmentAccessDeniedException("User is not in the appointment audience");
        }
    }

    @Transactional(readOnly = true)
    public Set<Long> resolveAudienceUserIds(Appointment appointment) {
        Long organizationId = appointment.getOrganization().getId();
        if (appointment.getAudienceScope() == AudienceScope.ALL) {
            return userOrganizationMembershipRepository
                    .findActiveByOrganization_Id(organizationId, Instant.now())
                    .stream()
                    .map(m -> m.getUser().getId())
                    .collect(Collectors.toSet());
        }
        if (appointment.getAudienceGroupIds() == null
                || appointment.getAudienceGroupIds().isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(
                groupMembershipRepository.findActiveUserIdsByGroupIds(
                        appointment.getAudienceGroupIds(), Instant.now()));
    }
}
