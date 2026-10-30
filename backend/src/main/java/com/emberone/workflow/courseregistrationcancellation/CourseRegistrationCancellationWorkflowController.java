/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.courseregistrationcancellation;

import com.emberone.workflow.domain.WorkflowType;
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
 * Self-service: cancel a participant place of a course offering. Whether the registration is the
 * caller's own and still cancellable is validated by the handler when the workflow opens.
 */
@RestController
@RequestMapping("/api/v1/auth/workflows/course-registration-cancellation")
@RequiredArgsConstructor
public class CourseRegistrationCancellationWorkflowController {

    private final WorkflowService workflowService;

    @PostMapping
    @PreAuthorize("@securityService.isSelfServiceUser()")
    public ResponseEntity<WorkflowDto> open(
            @Valid @RequestBody CourseRegistrationCancellationWorkflowPayload payload) {
        WorkflowDto dto =
                workflowService.open(
                        WorkflowType.COURSE_REGISTRATION_CANCELLATION.getId(),
                        payload,
                        CourseRegistrationCancellationWorkflowHandler.SUBJECT_ENTITY_TYPE,
                        payload.getRegistrationId());
        return ResponseEntity.ok(dto);
    }
}
