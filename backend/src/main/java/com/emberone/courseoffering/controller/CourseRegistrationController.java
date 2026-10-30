/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.controller;

import com.emberone.courseoffering.dto.CourseRegistrationDto;
import com.emberone.courseoffering.dto.CourseRegistrationPageDto;
import com.emberone.courseoffering.dto.CourseRegistrationSummaryDto;
import com.emberone.courseoffering.dto.RemoveCourseRegistrationRequest;
import com.emberone.courseoffering.list.CourseRegistrationListFilter;
import com.emberone.courseoffering.service.CourseRegistrationQueryService;
import com.emberone.courseoffering.service.CourseRegistrationService;
import com.emberone.platform.web.list.ListQuery;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Participants, waitlist and open registrations of an offering (offering side). */
@RestController
@RequestMapping(
        "/api/v1/auth/organizations/{organizationId}/course-offerings/{offeringId}/registrations")
@RequiredArgsConstructor
public class CourseRegistrationController {

    private final CourseRegistrationService registrationService;
    private final CourseRegistrationQueryService queryService;

    @GetMapping
    @PreAuthorize("@securityService.hasCourseOfferingReadPermission(#organizationId)")
    public ResponseEntity<CourseRegistrationPageDto> list(
            @PathVariable Long organizationId,
            @PathVariable Long offeringId,
            ListQuery query,
            @RequestParam(required = false) List<String> status) {
        return ResponseEntity.ok(
                queryService.listForOffering(
                        organizationId,
                        offeringId,
                        query,
                        CourseRegistrationListFilter.parse(status)));
    }

    @GetMapping("/{registrationId}")
    @PreAuthorize("@securityService.hasCourseOfferingReadPermission(#organizationId)")
    public ResponseEntity<CourseRegistrationDto> get(
            @PathVariable Long organizationId,
            @PathVariable Long offeringId,
            @PathVariable Long registrationId) {
        return ResponseEntity.ok(
                queryService.getForOffering(organizationId, offeringId, registrationId));
    }

    @GetMapping("/summary")
    @PreAuthorize("@securityService.hasCourseOfferingReadPermission(#organizationId)")
    public ResponseEntity<CourseRegistrationSummaryDto> summary(
            @PathVariable Long organizationId, @PathVariable Long offeringId) {
        return ResponseEntity.ok(queryService.summary(organizationId, offeringId));
    }

    @PostMapping("/{registrationId}/promote")
    @PreAuthorize("@securityService.hasCourseOfferingWritePermission(#organizationId)")
    public ResponseEntity<CourseRegistrationDto> promote(
            @PathVariable Long organizationId,
            @PathVariable Long offeringId,
            @PathVariable Long registrationId) {
        return ResponseEntity.ok(
                registrationService.promote(organizationId, offeringId, registrationId));
    }

    @PostMapping("/{registrationId}/remove")
    @PreAuthorize("@securityService.hasCourseOfferingWritePermission(#organizationId)")
    public ResponseEntity<CourseRegistrationDto> remove(
            @PathVariable Long organizationId,
            @PathVariable Long offeringId,
            @PathVariable Long registrationId,
            @Valid @RequestBody RemoveCourseRegistrationRequest request) {
        return ResponseEntity.ok(
                registrationService.remove(
                        organizationId, offeringId, registrationId, request.getReason()));
    }
}
