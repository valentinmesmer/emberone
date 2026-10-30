/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.drivinglicense;

import com.emberone.platform.security.TokenService;
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

@RestController
@RequestMapping("/api/v1/auth/workflows/driving-license-record")
@RequiredArgsConstructor
public class DrivingLicenseRecordWorkflowController {

    private final WorkflowService workflowService;
    private final TokenService tokenService;

    @PostMapping
    @PreAuthorize(
            "@securityService.canBeginSelfServiceWorkflow(T(com.emberone.workflow.domain.WorkflowType).DRIVING_LICENSE_RECORD.getId())")
    public ResponseEntity<WorkflowDto> open(
            @Valid @RequestBody DrivingLicenseRecordWorkflowPayload payload) {
        Long openerId = tokenService.requireUserId();
        WorkflowDto dto =
                workflowService.open(
                        WorkflowType.DRIVING_LICENSE_RECORD.getId(), payload, "USER", openerId);
        return ResponseEntity.ok(dto);
    }
}
