/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.event.controller;

import com.emberone.certification.dto.CertificationEventGrantDetailDto;
import com.emberone.certification.dto.CertificationEventGrantDto;
import com.emberone.certification.dto.CreateCertificationEventGrantRequest;
import com.emberone.certification.dto.UpdateCertificationEventGrantRequest;
import com.emberone.certification.service.CertificationEventGrantService;
import com.emberone.event.domain.EventType;
import com.emberone.event.util.EventCategoryPath;
import com.emberone.platform.web.list.BoundedList;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/events")
@RequiredArgsConstructor
public class EventCertificationGrantController {

    private final CertificationEventGrantService grantService;

    @GetMapping("/{eventCategory}/{eventId}/certification-grants")
    @BoundedList("Certification grants per event")
    @PreAuthorize("@securityService.hasEventCertificationGrantReadPermission(#organizationId)")
    public ResponseEntity<List<CertificationEventGrantDto>> listGrants(
            @PathVariable Long organizationId,
            @PathVariable String eventCategory,
            @PathVariable Long eventId) {
        EventType eventType = EventCategoryPath.toEventType(eventCategory);
        return ResponseEntity.ok(grantService.listForEvent(organizationId, eventType, eventId));
    }

    @GetMapping("/{eventCategory}/{eventId}/certification-grants/{grantId}")
    @PreAuthorize("@securityService.hasEventCertificationGrantReadPermission(#organizationId)")
    public ResponseEntity<CertificationEventGrantDetailDto> getGrant(
            @PathVariable Long organizationId,
            @PathVariable String eventCategory,
            @PathVariable Long eventId,
            @PathVariable Long grantId) {
        EventType eventType = EventCategoryPath.toEventType(eventCategory);
        return ResponseEntity.ok(
                grantService.getGrant(organizationId, eventType, eventId, grantId));
    }

    @PostMapping("/{eventCategory}/{eventId}/certification-grants")
    @PreAuthorize("@securityService.hasEventCertificationGrantWritePermission(#organizationId)")
    public ResponseEntity<CertificationEventGrantDetailDto> createGrant(
            @PathVariable Long organizationId,
            @PathVariable String eventCategory,
            @PathVariable Long eventId,
            @Valid @RequestBody CreateCertificationEventGrantRequest request) {
        EventType eventType = EventCategoryPath.toEventType(eventCategory);
        CertificationEventGrantDetailDto result =
                grantService.createGrant(organizationId, eventType, eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/{eventCategory}/{eventId}/certification-grants/{grantId}")
    @PreAuthorize("@securityService.hasEventCertificationGrantWritePermission(#organizationId)")
    public ResponseEntity<CertificationEventGrantDetailDto> updateGrant(
            @PathVariable Long organizationId,
            @PathVariable String eventCategory,
            @PathVariable Long eventId,
            @PathVariable Long grantId,
            @Valid @RequestBody UpdateCertificationEventGrantRequest request) {
        EventType eventType = EventCategoryPath.toEventType(eventCategory);
        return ResponseEntity.ok(
                grantService.updateGrant(organizationId, eventType, eventId, grantId, request));
    }

    @DeleteMapping("/{eventCategory}/{eventId}/certification-grants/{grantId}")
    @PreAuthorize("@securityService.hasEventCertificationGrantWritePermission(#organizationId)")
    public ResponseEntity<Void> deleteGrant(
            @PathVariable Long organizationId,
            @PathVariable String eventCategory,
            @PathVariable Long eventId,
            @PathVariable Long grantId) {
        EventType eventType = EventCategoryPath.toEventType(eventCategory);
        grantService.deleteGrant(organizationId, eventType, eventId, grantId);
        return ResponseEntity.noContent().build();
    }
}
