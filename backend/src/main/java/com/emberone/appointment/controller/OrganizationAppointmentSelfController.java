/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.controller;

import com.emberone.appointment.dto.AppointmentResponseDto;
import com.emberone.appointment.dto.SubmitAppointmentResponseRequest;
import com.emberone.appointment.service.AppointmentResponseService;
import com.emberone.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/self/organizations/{organizationId}/appointments")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class OrganizationAppointmentSelfController {

    private final AppointmentService appointmentService;
    private final AppointmentResponseService appointmentResponseService;

    @PostMapping("/{appointmentId}/responses")
    @PreAuthorize("@securityService.isActiveOrganizationMember(#organizationId)")
    public ResponseEntity<AppointmentResponseDto> submitResponse(
            @PathVariable Long organizationId,
            @PathVariable Long appointmentId,
            @Valid @RequestBody SubmitAppointmentResponseRequest request) {
        return ResponseEntity.ok(
                appointmentResponseService.submitResponse(appointmentId, organizationId, request));
    }

    @PostMapping("/{appointmentId}/mark-read")
    @PreAuthorize("@securityService.isActiveOrganizationMember(#organizationId)")
    public ResponseEntity<Void> markRead(
            @PathVariable Long organizationId, @PathVariable Long appointmentId) {
        appointmentService.markRead(organizationId, appointmentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mark-all-read")
    @PreAuthorize("@securityService.isActiveOrganizationMember(#organizationId)")
    public ResponseEntity<Void> markAllRead(@PathVariable Long organizationId) {
        appointmentService.markAllReadForOrganization(organizationId);
        return ResponseEntity.noContent().build();
    }
}
