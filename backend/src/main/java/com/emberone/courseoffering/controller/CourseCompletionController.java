/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.controller;

import com.emberone.courseoffering.dto.CourseCompletionDto;
import com.emberone.courseoffering.dto.SetCourseFinalResultRequest;
import com.emberone.courseoffering.service.CourseCompletionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Completion list of an offering: manual final results and confirmation (offering side). */
@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/course-offerings/{offeringId}")
@RequiredArgsConstructor
public class CourseCompletionController {

    private final CourseCompletionService completionService;

    @GetMapping("/completion")
    @PreAuthorize("@securityService.hasCourseOfferingReadPermission(#organizationId)")
    public ResponseEntity<CourseCompletionDto> get(
            @PathVariable Long organizationId, @PathVariable Long offeringId) {
        return ResponseEntity.ok(completionService.get(organizationId, offeringId));
    }

    @PutMapping("/registrations/{registrationId}/final-result")
    @PreAuthorize("@securityService.hasCourseOfferingWritePermission(#organizationId)")
    public ResponseEntity<CourseCompletionDto> setFinalResult(
            @PathVariable Long organizationId,
            @PathVariable Long offeringId,
            @PathVariable Long registrationId,
            @Valid @RequestBody SetCourseFinalResultRequest request) {
        return ResponseEntity.ok(
                completionService.setFinalResult(
                        organizationId, offeringId, registrationId, request.getPassed()));
    }

    @PostMapping("/complete")
    @PreAuthorize("@securityService.hasCourseOfferingWritePermission(#organizationId)")
    public ResponseEntity<CourseCompletionDto> complete(
            @PathVariable Long organizationId, @PathVariable Long offeringId) {
        return ResponseEntity.ok(completionService.complete(organizationId, offeringId));
    }
}
