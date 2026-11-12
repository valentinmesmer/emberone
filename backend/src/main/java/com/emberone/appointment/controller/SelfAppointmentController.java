/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.appointment.controller;

import com.emberone.appointment.dto.UpcomingAppointmentsDto;
import com.emberone.appointment.service.AppointmentService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/self/appointments")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class SelfAppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping("/upcoming")
    public ResponseEntity<UpcomingAppointmentsDto> getUpcomingAppointments(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(appointmentService.listUpcomingAcrossOrganizations(limit));
    }

    @GetMapping("/my-unread-counts")
    public ResponseEntity<Map<Long, Long>> getMyUnreadCounts() {
        return ResponseEntity.ok(appointmentService.countUnreadAcrossOrganizations());
    }
}
