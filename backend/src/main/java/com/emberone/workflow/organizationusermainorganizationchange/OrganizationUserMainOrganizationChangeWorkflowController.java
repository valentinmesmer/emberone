/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.organizationusermainorganizationchange;

import com.emberone.platform.security.TokenService;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.OpenOrganizationUserMainOrganizationChangeWorkflowRequest;
import com.emberone.workflow.dto.OrganizationUserMainOrganizationChangeWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDto;
import com.emberone.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/users/{userId}/workflows")
@RequiredArgsConstructor
public class OrganizationUserMainOrganizationChangeWorkflowController {

    private final WorkflowService workflowService;
    private final TokenService tokenService;

    @PostMapping("/organization-user-main-organization-change")
    @PreAuthorize(
            "@securityService.canOpenOrganizationUserMainOrganizationChangeWorkflow(#organizationId,"
                    + " #userId)")
    public ResponseEntity<WorkflowDto> open(
            @PathVariable long organizationId,
            @PathVariable long userId,
            @Valid @RequestBody OpenOrganizationUserMainOrganizationChangeWorkflowRequest request) {
        tokenService.requireUserId();
        OrganizationUserMainOrganizationChangeWorkflowPayload payload =
                OrganizationUserMainOrganizationChangeWorkflowPayload.builder()
                        .organizationId(organizationId)
                        .targetUserId(userId)
                        .newMainOrganizationId(request.getNewMainOrganizationId())
                        .reason(request.getReason())
                        .build();
        WorkflowDto dto =
                workflowService.open(
                        WorkflowType.ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE.getId(),
                        payload,
                        OrganizationUserMainOrganizationChangeWorkflowHandler.SUBJECT_ENTITY_TYPE,
                        userId);
        return ResponseEntity.ok(dto);
    }
}
