/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.controller;

import com.emberone.event.domain.EventType;
import com.emberone.event.dto.*;
import com.emberone.event.list.EventListFilter;
import com.emberone.event.list.EventOrganizationListFilterOptions;
import com.emberone.event.service.EventAttendanceService;
import com.emberone.event.service.EventService;
import com.emberone.event.util.EventCategoryPath;
import com.emberone.platform.security.TokenService;
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
@RequestMapping("/api/v1/auth/organizations/{organizationId}/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final TokenService tokenService;
    private final EventAttendanceService eventAttendanceService;
    private final EventOrganizationListFilterOptions organizationListFilterOptions;

    @GetMapping("/{eventCategory}/{eventId}/attendances")
    @PreAuthorize("@securityService.hasEventAttendanceReadPermission(#organizationId)")
    public ResponseEntity<EventAttendancePageDto> listAttendances(
            @PathVariable Long organizationId,
            @PathVariable String eventCategory,
            @PathVariable Long eventId,
            ListQuery query) {
        EventType eventType = EventCategoryPath.toEventType(eventCategory);
        return ResponseEntity.ok(
                eventAttendanceService.listAttendances(organizationId, eventType, eventId, query));
    }

    @PostMapping("/{eventCategory}/{eventId}/attendances")
    @PreAuthorize("@securityService.hasEventAttendanceWritePermission(#organizationId)")
    public ResponseEntity<EventAttendanceDto> addAttendance(
            @PathVariable Long organizationId,
            @PathVariable String eventCategory,
            @PathVariable Long eventId,
            @Valid @RequestBody CreateEventAttendanceRequest request) {
        EventType eventType = EventCategoryPath.toEventType(eventCategory);
        EventAttendanceDto created =
                eventAttendanceService.addAttendance(organizationId, eventType, eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{eventCategory}/{eventId}/attendances/{attendanceId}")
    @PreAuthorize("@securityService.hasEventAttendanceWritePermission(#organizationId)")
    public ResponseEntity<Void> removeAttendance(
            @PathVariable Long organizationId,
            @PathVariable String eventCategory,
            @PathVariable Long eventId,
            @PathVariable Long attendanceId) {
        EventType eventType = EventCategoryPath.toEventType(eventCategory);
        eventAttendanceService.removeAttendance(organizationId, eventType, eventId, attendanceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/service/list-filter-options")
    @PreAuthorize("@securityService.hasServiceEventReadPermission(#organizationId)")
    public ResponseEntity<ListFilterOptionsDto> listServiceEventFilterOptions(
            @PathVariable Long organizationId) {
        return ResponseEntity.ok(
                organizationListFilterOptions.forOrganization(organizationId, EventType.SERVICE));
    }

    @GetMapping("/operation/list-filter-options")
    @PreAuthorize("@securityService.hasOperationEventReadPermission(#organizationId)")
    public ResponseEntity<ListFilterOptionsDto> listOperationEventFilterOptions(
            @PathVariable Long organizationId) {
        return ResponseEntity.ok(
                organizationListFilterOptions.forOrganization(organizationId, EventType.OPERATION));
    }

    @GetMapping("/service/{eventId}")
    @PreAuthorize("@securityService.hasServiceEventReadPermission(#organizationId)")
    public ResponseEntity<ServiceEventDto> getServiceEvent(
            @PathVariable Long organizationId, @PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getServiceEvent(organizationId, eventId));
    }

    @GetMapping("/operation/{eventId}")
    @PreAuthorize("@securityService.hasOperationEventReadPermission(#organizationId)")
    public ResponseEntity<OperationEventDto> getOperationEvent(
            @PathVariable Long organizationId, @PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getOperationEvent(organizationId, eventId));
    }

    @GetMapping("/service")
    @PreAuthorize("@securityService.hasServiceEventReadPermission(#organizationId)")
    public ResponseEntity<ServiceEventPageDto> listServiceEvents(
            @PathVariable Long organizationId,
            ListQuery query,
            @RequestParam(required = false) List<Integer> year,
            @RequestParam(required = false) List<String> attendance,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        EventListFilter filter =
                from != null || to != null
                        ? EventListFilter.forDateRange(from, to)
                        : EventListFilter.parseOrg(year, attendance);
        return ResponseEntity.ok(eventService.listServiceEvents(organizationId, query, filter));
    }

    @GetMapping("/operation")
    @PreAuthorize("@securityService.hasOperationEventReadPermission(#organizationId)")
    public ResponseEntity<OperationEventPageDto> listOperationEvents(
            @PathVariable Long organizationId,
            ListQuery query,
            @RequestParam(required = false) List<Integer> year,
            @RequestParam(required = false) List<String> attendance,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        EventListFilter filter =
                from != null || to != null
                        ? EventListFilter.forDateRange(from, to)
                        : EventListFilter.parseOrg(year, attendance);
        return ResponseEntity.ok(eventService.listOperationEvents(organizationId, query, filter));
    }

    @PostMapping("/service")
    @PreAuthorize("@securityService.hasServiceEventWritePermission(#organizationId)")
    public ResponseEntity<ServiceEventDto> createServiceEvent(
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateServiceEventRequest request) {
        Long userId = tokenService.requireUserId();
        ServiceEventDto created = eventService.createServiceEvent(userId, organizationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/operation")
    @PreAuthorize("@securityService.hasOperationEventWritePermission(#organizationId)")
    public ResponseEntity<OperationEventDto> createOperationEvent(
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateOperationEventRequest request) {
        Long userId = tokenService.requireUserId();
        OperationEventDto created =
                eventService.createOperationEvent(userId, organizationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/service/{eventId}")
    @PreAuthorize("@securityService.hasServiceEventWritePermission(#organizationId)")
    public ResponseEntity<ServiceEventDto> updateServiceEvent(
            @PathVariable Long organizationId,
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateServiceEventRequest request) {
        Long userId = tokenService.requireUserId();
        return ResponseEntity.ok(
                eventService.updateServiceEvent(userId, organizationId, eventId, request));
    }

    @DeleteMapping("/service/{eventId}")
    @PreAuthorize("@securityService.hasServiceEventWritePermission(#organizationId)")
    public ResponseEntity<Void> deleteServiceEvent(
            @PathVariable Long organizationId, @PathVariable Long eventId) {
        Long userId = tokenService.requireUserId();
        eventService.deleteServiceEvent(userId, organizationId, eventId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/operation/{eventId}")
    @PreAuthorize("@securityService.hasOperationEventWritePermission(#organizationId)")
    public ResponseEntity<OperationEventDto> updateOperationEvent(
            @PathVariable Long organizationId,
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateOperationEventRequest request) {
        Long userId = tokenService.requireUserId();
        return ResponseEntity.ok(
                eventService.updateOperationEvent(userId, organizationId, eventId, request));
    }

    @DeleteMapping("/operation/{eventId}")
    @PreAuthorize("@securityService.hasOperationEventWritePermission(#organizationId)")
    public ResponseEntity<Void> deleteOperationEvent(
            @PathVariable Long organizationId, @PathVariable Long eventId) {
        Long userId = tokenService.requireUserId();
        eventService.deleteOperationEvent(userId, organizationId, eventId);
        return ResponseEntity.noContent().build();
    }
}
