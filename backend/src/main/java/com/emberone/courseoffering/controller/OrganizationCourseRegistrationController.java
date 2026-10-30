/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.controller;

import com.emberone.courseoffering.dto.OrganizationCourseRegistrationPageDto;
import com.emberone.courseoffering.list.UserCourseRegistrationListFilter;
import com.emberone.courseoffering.service.CourseRegistrationQueryService;
import com.emberone.platform.web.list.ListQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Registrations for the offerings of an organization, listed in the members area. Guarded by the
 * training-course read permission, like the member view; the course-offering permissions are not
 * needed to see who registered.
 */
@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/course-registrations")
@RequiredArgsConstructor
public class OrganizationCourseRegistrationController {

    private final CourseRegistrationQueryService queryService;

    @GetMapping
    @PreAuthorize("@securityService.hasTrainingCourseReadPermission(#organizationId)")
    public ResponseEntity<OrganizationCourseRegistrationPageDto> list(
            @PathVariable Long organizationId,
            ListQuery query,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<String> upcoming) {
        return ResponseEntity.ok(
                queryService.listForOrganization(
                        organizationId,
                        query,
                        UserCourseRegistrationListFilter.parse(status, upcoming)));
    }
}
