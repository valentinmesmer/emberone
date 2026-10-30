/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.controller;

import com.emberone.courseoffering.dto.CoursePrerequisiteCheckDto;
import com.emberone.courseoffering.dto.SelfCourseOfferingDto;
import com.emberone.courseoffering.dto.SelfCourseOfferingPageDto;
import com.emberone.courseoffering.list.SelfCourseOfferingListFilter;
import com.emberone.courseoffering.list.SelfCourseOfferingParticipationState;
import com.emberone.courseoffering.list.SelfCourseOfferingRegistrationState;
import com.emberone.courseoffering.service.SelfCourseOfferingService;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Offerings the caller may register for. */
@RestController
@RequestMapping("/api/v1/self/course-offerings")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class CourseOfferingSelfController {

    private final SelfCourseOfferingService selfService;
    private final TokenService tokenService;

    @GetMapping("/{offeringId}/prerequisites")
    public ResponseEntity<CoursePrerequisiteCheckDto> checkPrerequisites(
            @PathVariable Long offeringId) {
        return ResponseEntity.ok(
                selfService.checkPrerequisites(tokenService.requireUserId(), offeringId));
    }

    @GetMapping("/{offeringId}")
    public ResponseEntity<SelfCourseOfferingDto> get(@PathVariable Long offeringId) {
        return ResponseEntity.ok(selfService.get(tokenService.requireUserId(), offeringId));
    }

    @GetMapping
    public ResponseEntity<SelfCourseOfferingPageDto> list(
            ListQuery query,
            @RequestParam(defaultValue = "false") boolean includeHeld,
            @RequestParam(defaultValue = "false") boolean includeUnfulfilled,
            @RequestParam(name = "registration", required = false)
                    List<SelfCourseOfferingRegistrationState> registration,
            @RequestParam(name = "participation", required = false)
                    List<SelfCourseOfferingParticipationState> participation) {
        return ResponseEntity.ok(
                selfService.list(
                        tokenService.requireUserId(),
                        query,
                        new SelfCourseOfferingListFilter(
                                includeHeld,
                                includeUnfulfilled,
                                registration == null ? Set.of() : Set.copyOf(registration),
                                participation == null ? Set.of() : Set.copyOf(participation))));
    }
}
