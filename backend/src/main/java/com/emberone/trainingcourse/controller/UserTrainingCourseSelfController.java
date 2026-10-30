/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.controller;

import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.BoundedList;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.trainingcourse.dto.TrainingCourseTypeDto;
import com.emberone.trainingcourse.dto.TrainingCourseUserRecordPageDto;
import com.emberone.trainingcourse.list.TrainingCourseRecordListFilter;
import com.emberone.trainingcourse.service.TrainingCourseCatalogService;
import com.emberone.trainingcourse.service.TrainingCourseRecordService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service training course reads: own records and membership-org catalog types. Submitting
 * proofs uses {@link com.emberone.workflow.trainingcourse.TrainingCourseRecordWorkflowController}.
 * HR management of other users uses {@link UserTrainingCourseOrganizationController}.
 */
@RestController
@RequestMapping("/api/v1/self/training-courses")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class UserTrainingCourseSelfController {

    private final TrainingCourseRecordService recordService;
    private final TrainingCourseCatalogService catalogService;
    private final TokenService tokenService;

    @GetMapping("/records")
    public ResponseEntity<TrainingCourseUserRecordPageDto> getMyRecords(
            ListQuery query, TrainingCourseRecordListFilter filter) {
        return ResponseEntity.ok(
                recordService.listRecords(tokenService.requireUserId(), query, filter));
    }

    /** Catalog types for the caller's membership organization (for Nachweis einreichen). */
    @GetMapping("/available-types")
    @BoundedList("Org-standard training course type catalog")
    public ResponseEntity<List<TrainingCourseTypeDto>> getMyAvailableTypes() {
        return ResponseEntity.ok(
                catalogService.listAvailableTypesForUser(tokenService.requireUserId()));
    }
}
