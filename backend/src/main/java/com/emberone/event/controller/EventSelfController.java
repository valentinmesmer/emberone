/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.controller;

import com.emberone.event.domain.EventType;
import com.emberone.event.dto.EventAttendanceSelfItemDto;
import com.emberone.event.service.EventAttendanceService;
import com.emberone.event.util.EventCategoryPath;
import com.emberone.platform.web.list.BoundedList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/self/organizations/{organizationId}/events")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class EventSelfController {

    private final EventAttendanceService eventAttendanceService;

    @GetMapping("/{eventCategory}/attendances/me")
    @BoundedList("Batch attendance lookup by eventIds")
    @PreAuthorize("@securityService.isActiveOrganizationMember(#organizationId)")
    public ResponseEntity<List<EventAttendanceSelfItemDto>> getMyAttendances(
            @PathVariable Long organizationId,
            @PathVariable String eventCategory,
            @RequestParam List<Long> eventIds) {
        EventType eventType = EventCategoryPath.toEventType(eventCategory);
        return ResponseEntity.ok(
                eventAttendanceService.getAttendancesForUser(organizationId, eventType, eventIds));
    }
}
