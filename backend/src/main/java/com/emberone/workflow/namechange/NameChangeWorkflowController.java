/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.namechange;

import com.emberone.platform.ai.extraction.DocumentExtractionResult;
import com.emberone.platform.ai.extraction.DocumentExtractionService;
import com.emberone.platform.security.TokenService;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.NameChangeWorkflowPayload;
import com.emberone.workflow.dto.WorkflowDto;
import com.emberone.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Self-service: request a name change (upload attachments via document API, then open the
 * workflow).
 */
@RestController
@RequestMapping("/api/v1/auth/workflows/name-change")
@RequiredArgsConstructor
public class NameChangeWorkflowController {

    private final WorkflowService workflowService;
    private final TokenService tokenService;
    private final DocumentExtractionService documentExtractionService;

    @PostMapping
    @PreAuthorize(
            "@securityService.canBeginSelfServiceWorkflow(T(com.emberone.workflow.domain.WorkflowType).NAME_CHANGE.getId())")
    public ResponseEntity<WorkflowDto> open(@Valid @RequestBody NameChangeWorkflowPayload payload) {
        Long openerId = tokenService.requireUserId();
        WorkflowDto dto =
                workflowService.open(WorkflowType.NAME_CHANGE.getId(), payload, "USER", openerId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Optional AI assist: suggests form values from the uploaded proof images. Same permission as
     * opening the workflow; nothing is stored, the user reviews and submits the form themselves.
     */
    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(
            "@securityService.canBeginSelfServiceWorkflow(T(com.emberone.workflow.domain.WorkflowType).NAME_CHANGE.getId())")
    public ResponseEntity<DocumentExtractionResult> extract(
            @RequestParam("file") List<MultipartFile> files) {
        return ResponseEntity.ok(
                documentExtractionService.extract(WorkflowType.NAME_CHANGE.name(), files, null));
    }
}
