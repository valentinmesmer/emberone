/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.controller;

import com.emberone.courseoffering.dto.SelfCourseRegistrationDto;
import com.emberone.courseoffering.dto.SelfCourseRegistrationPageDto;
import com.emberone.courseoffering.service.CourseRegistrationQueryService;
import com.emberone.courseoffering.service.CourseRegistrationService;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * The caller's own course registrations. Cancelling a participant place goes through the {@code
 * course-registration-cancellation} workflow; only the waitlist is left directly.
 */
@RestController
@RequestMapping("/api/v1/self/course-registrations")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class CourseRegistrationSelfController {

    private final CourseRegistrationQueryService queryService;
    private final CourseRegistrationService registrationService;
    private final TokenService tokenService;

    @GetMapping
    public ResponseEntity<SelfCourseRegistrationPageDto> list(ListQuery query) {
        return ResponseEntity.ok(queryService.listForSelf(tokenService.requireUserId(), query));
    }

    @GetMapping("/{registrationId}")
    public ResponseEntity<SelfCourseRegistrationDto> get(@PathVariable Long registrationId) {
        return ResponseEntity.ok(
                queryService.getForSelf(tokenService.requireUserId(), registrationId));
    }

    @PostMapping("/{registrationId}/withdraw")
    public ResponseEntity<Void> withdrawFromWaitlist(@PathVariable Long registrationId) {
        registrationService.withdrawFromWaitlist(tokenService.requireUserId(), registrationId);
        return ResponseEntity.noContent().build();
    }
}
