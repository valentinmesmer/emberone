/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.controller;

import com.emberone.appointment.domain.SeriesEditScope;
import com.emberone.appointment.dto.*;
import com.emberone.appointment.list.AppointmentListFilter;
import com.emberone.appointment.list.AppointmentOrganizationListFilterOptions;
import com.emberone.appointment.service.AppointmentResponseService;
import com.emberone.appointment.service.AppointmentService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentResponseService appointmentResponseService;
    private final AppointmentOrganizationListFilterOptions organizationListFilterOptions;

    @GetMapping("/list-filter-options")
    @PreAuthorize("@securityService.hasAppointmentReadPermission(#organizationId)")
    public ResponseEntity<ListFilterOptionsDto> listFilterOptions(
            @PathVariable Long organizationId) {
        return ResponseEntity.ok(organizationListFilterOptions.forOrganization(organizationId));
    }

    @GetMapping
    @PreAuthorize("@securityService.hasAppointmentReadPermission(#organizationId)")
    public ResponseEntity<AppointmentPageDto> listAppointments(
            @PathVariable Long organizationId,
            ListQuery query,
            @RequestParam(required = false) List<String> period,
            @RequestParam(required = false) List<Integer> year,
            @RequestParam(required = false) List<String> viewed,
            @RequestParam(required = false) List<String> response,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        AppointmentListFilter filter =
                from != null || to != null
                        ? AppointmentListFilter.forDateRange(from, to)
                        : AppointmentListFilter.parseOrg(period, year, viewed, response);
        return ResponseEntity.ok(
                appointmentService.listAppointments(organizationId, query, filter));
    }

    @GetMapping("/{appointmentId}")
    @PreAuthorize("@securityService.hasAppointmentReadPermission(#organizationId)")
    public ResponseEntity<AppointmentDto> getAppointment(
            @PathVariable Long organizationId, @PathVariable Long appointmentId) {
        return ResponseEntity.ok(appointmentService.getAppointment(organizationId, appointmentId));
    }

    @PostMapping
    @PreAuthorize("@securityService.hasAppointmentWritePermission(#organizationId)")
    public ResponseEntity<AppointmentDto> createAppointment(
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateAppointmentRequest request) {
        AppointmentDto created = appointmentService.createAppointment(organizationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{appointmentId}")
    @PreAuthorize("@securityService.hasAppointmentWritePermission(#organizationId)")
    public ResponseEntity<AppointmentDto> updateAppointment(
            @PathVariable Long organizationId,
            @PathVariable Long appointmentId,
            @RequestParam(required = false) SeriesEditScope scope,
            @Valid @RequestBody UpdateAppointmentRequest request) {
        return ResponseEntity.ok(
                appointmentService.updateAppointment(
                        organizationId, appointmentId, request, scope));
    }

    @DeleteMapping("/{appointmentId}")
    @PreAuthorize("@securityService.hasAppointmentWritePermission(#organizationId)")
    public ResponseEntity<Void> deleteAppointment(
            @PathVariable Long organizationId,
            @PathVariable Long appointmentId,
            @RequestParam(required = false) SeriesEditScope scope) {
        appointmentService.deleteAppointment(organizationId, appointmentId, scope);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{appointmentId}/participation-stats")
    @PreAuthorize("@securityService.hasAppointmentReadPermission(#organizationId)")
    public ResponseEntity<AppointmentParticipationStatsDto> getParticipationStats(
            @PathVariable Long organizationId, @PathVariable Long appointmentId) {
        return ResponseEntity.ok(
                appointmentResponseService.getParticipationStats(appointmentId, organizationId));
    }
}
