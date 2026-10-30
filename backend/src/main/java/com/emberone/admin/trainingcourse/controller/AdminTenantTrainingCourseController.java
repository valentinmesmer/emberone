/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.trainingcourse.controller;

import com.emberone.platform.web.list.BoundedList;
import com.emberone.trainingcourse.dto.CreateTenantTrainingCourseTypeRequest;
import com.emberone.trainingcourse.dto.TenantTrainingCourseTypeDto;
import com.emberone.trainingcourse.dto.UpdateTenantTrainingCourseTypeRequest;
import com.emberone.trainingcourse.service.TenantTrainingCourseTypeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/admin/tenants/{tenantId}/training-course-types")
@RequiredArgsConstructor
public class AdminTenantTrainingCourseController {

    private final TenantTrainingCourseTypeService tenantTrainingCourseTypeService;

    @GetMapping
    @BoundedList("Tenant training course type catalog")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<List<TenantTrainingCourseTypeDto>> list(@PathVariable long tenantId) {
        return ResponseEntity.ok(tenantTrainingCourseTypeService.list(tenantId));
    }

    @GetMapping("/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantTrainingCourseTypeDto> get(
            @PathVariable long tenantId, @PathVariable String configKey) {
        return ResponseEntity.ok(tenantTrainingCourseTypeService.get(tenantId, configKey));
    }

    @PostMapping
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantTrainingCourseTypeDto> create(
            @PathVariable long tenantId,
            @Valid @RequestBody CreateTenantTrainingCourseTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tenantTrainingCourseTypeService.create(tenantId, request));
    }

    @PutMapping("/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantTrainingCourseTypeDto> update(
            @PathVariable long tenantId,
            @PathVariable String configKey,
            @Valid @RequestBody UpdateTenantTrainingCourseTypeRequest request) {
        return ResponseEntity.ok(
                tenantTrainingCourseTypeService.update(tenantId, configKey, request));
    }

    @DeleteMapping("/{configKey}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> delete(
            @PathVariable long tenantId, @PathVariable String configKey) {
        tenantTrainingCourseTypeService.delete(tenantId, configKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{configKey}/archive")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> archive(
            @PathVariable long tenantId, @PathVariable String configKey) {
        tenantTrainingCourseTypeService.archive(tenantId, configKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{configKey}/unarchive")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> unarchive(
            @PathVariable long tenantId, @PathVariable String configKey) {
        tenantTrainingCourseTypeService.unarchive(tenantId, configKey);
        return ResponseEntity.noContent().build();
    }
}
