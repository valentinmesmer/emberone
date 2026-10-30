/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.organization.controller;

import com.emberone.courseoffering.dto.CourseOfferingTypeSettingsDto;
import com.emberone.courseoffering.dto.ReplaceCourseOfferingTypesRequest;
import com.emberone.courseoffering.service.OrganizationCourseOfferingTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        "/api/v1/auth/admin/tenants/{tenantId}/organizations/{organizationId}/course-offering-types")
@RequiredArgsConstructor
public class AdminOrganizationCourseOfferingTypeController {

    private final OrganizationCourseOfferingTypeService courseOfferingTypeService;

    @GetMapping
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<CourseOfferingTypeSettingsDto> getCourseOfferingTypes(
            @PathVariable long tenantId, @PathVariable long organizationId) {
        return ResponseEntity.ok(courseOfferingTypeService.getSettings(tenantId, organizationId));
    }

    @PutMapping
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<CourseOfferingTypeSettingsDto> replaceCourseOfferingTypes(
            @PathVariable long tenantId,
            @PathVariable long organizationId,
            @Valid @RequestBody ReplaceCourseOfferingTypesRequest request) {
        return ResponseEntity.ok(
                courseOfferingTypeService.replace(
                        tenantId, organizationId, request.getDefinitionConfigKeys()));
    }
}
