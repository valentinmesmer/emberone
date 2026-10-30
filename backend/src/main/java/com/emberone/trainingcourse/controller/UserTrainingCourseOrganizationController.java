/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.controller;

import com.emberone.platform.web.list.BoundedList;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.trainingcourse.dto.*;
import com.emberone.trainingcourse.list.TrainingCourseRecordListFilter;
import com.emberone.trainingcourse.service.TrainingCourseCatalogService;
import com.emberone.trainingcourse.service.TrainingCourseRecordService;
import com.emberone.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Admin/manager training course API scoped to an organization. */
@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/users/{userId}/training-courses")
@RequiredArgsConstructor
public class UserTrainingCourseOrganizationController {

    private final TrainingCourseRecordService recordService;
    private final TrainingCourseCatalogService catalogService;
    private final UserService userService;

    @GetMapping("/records")
    @PreAuthorize("@securityService.hasTrainingCourseReadPermission(#organizationId)")
    public ResponseEntity<TrainingCourseUserRecordPageDto> listRecords(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            ListQuery query,
            TrainingCourseRecordListFilter filter) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(recordService.listRecords(userId, query, filter));
    }

    @GetMapping("/pending-submissions")
    @BoundedList("Open training-course workflow submissions per user")
    @PreAuthorize("@securityService.hasTrainingCourseReadPermission(#organizationId)")
    public ResponseEntity<List<TrainingCoursePendingSubmissionDto>> listPendingSubmissions(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(recordService.listPendingSubmissions(userId));
    }

    @PostMapping("/records")
    @PreAuthorize("@securityService.hasTrainingCourseWritePermission(#organizationId)")
    public ResponseEntity<TrainingCourseUserRecordDto> createRecord(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @Valid @RequestBody CreateTrainingCourseUserRecordRequest request) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recordService.createManual(organizationId, userId, request));
    }

    @PutMapping("/records/{recordId}")
    @PreAuthorize("@securityService.hasTrainingCourseWritePermission(#organizationId)")
    public ResponseEntity<TrainingCourseUserRecordDto> updateRecord(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable Long recordId,
            @Valid @RequestBody UpdateTrainingCourseUserRecordRequest request) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(
                recordService.updateManual(organizationId, userId, recordId, request));
    }

    @DeleteMapping("/records/{recordId}")
    @PreAuthorize("@securityService.hasTrainingCourseWritePermission(#organizationId)")
    public ResponseEntity<Void> deleteRecord(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable Long recordId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        recordService.deleteManual(organizationId, userId, recordId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/available-types")
    @BoundedList("Org-standard training course type catalog")
    @PreAuthorize("@securityService.hasTrainingCourseReadPermission(#organizationId)")
    public ResponseEntity<OrgTrainingCourseCatalogDto> getAvailableTypes(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        return ResponseEntity.ok(catalogService.getAvailableTypesForOrg(organizationId));
    }
}
