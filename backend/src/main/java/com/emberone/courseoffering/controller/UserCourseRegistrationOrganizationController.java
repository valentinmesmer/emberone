/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.controller;

import com.emberone.courseoffering.dto.UserCourseRegistrationPageDto;
import com.emberone.courseoffering.list.UserCourseRegistrationListFilter;
import com.emberone.courseoffering.service.CourseRegistrationQueryService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.user.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Course registrations of a member for the member view of an organization. Guarded by the
 * organization's training-course read permission, not by the course-offering permissions: the
 * registrations are member data and may belong to offerings of other organizations, so whether this
 * organization uses the course-offering module must not matter. That the member belongs to the
 * organization is enforced here and, for all {@code /organizations/{org}/users/{user}} paths, by
 * {@code OrganizationUserMembershipInterceptor}.
 */
@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/users/{userId}/course-registrations")
@RequiredArgsConstructor
public class UserCourseRegistrationOrganizationController {

    private final CourseRegistrationQueryService queryService;
    private final UserService userService;

    @GetMapping
    @PreAuthorize("@securityService.hasTrainingCourseReadPermission(#organizationId)")
    public ResponseEntity<UserCourseRegistrationPageDto> list(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            ListQuery query,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<String> upcoming) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(
                queryService.listForUser(
                        userId, query, UserCourseRegistrationListFilter.parse(status, upcoming)));
    }
}
