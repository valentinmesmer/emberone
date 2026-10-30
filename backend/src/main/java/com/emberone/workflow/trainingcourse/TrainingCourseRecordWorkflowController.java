/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.trainingcourse;

import com.emberone.platform.security.TokenService;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.TrainingCourseRecordWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDto;
import com.emberone.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service: submit a training course proof (upload attachments via the document API beforehand,
 * then open the workflow).
 */
@RestController
@RequestMapping("/api/v1/auth/workflows/training-course-record")
@RequiredArgsConstructor
public class TrainingCourseRecordWorkflowController {

    private final WorkflowService workflowService;
    private final TokenService tokenService;

    @PostMapping
    @PreAuthorize(
            "@securityService.canBeginSelfServiceWorkflow(T(com.emberone.workflow.domain.WorkflowType).TRAINING_COURSE_RECORD.getId())")
    public ResponseEntity<WorkflowDto> open(
            @Valid @RequestBody TrainingCourseRecordWorkflowPayload payload) {
        Long openerId = tokenService.requireUserId();
        WorkflowDto dto =
                workflowService.open(
                        WorkflowType.TRAINING_COURSE_RECORD.getId(), payload, "USER", openerId);
        return ResponseEntity.ok(dto);
    }
}
