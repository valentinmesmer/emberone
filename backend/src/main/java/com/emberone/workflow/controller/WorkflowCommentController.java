/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.controller;

import com.emberone.attachment.dto.EntityAttachmentDto;
import com.emberone.platform.web.list.BoundedList;
import com.emberone.workflow.dto.CreateWorkflowCommentRequest;
import com.emberone.workflow.dto.WorkflowCommentDto;
import com.emberone.workflow.service.WorkflowCommentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/auth/workflows")
@RequiredArgsConstructor
public class WorkflowCommentController {

    private final WorkflowCommentService commentService;

    @GetMapping("/{workflowId}/comments")
    @BoundedList("Comments on a single workflow instance")
    @PreAuthorize("@securityService.canView(#workflowId)")
    public ResponseEntity<List<WorkflowCommentDto>> listComments(@PathVariable Long workflowId) {
        return ResponseEntity.ok(commentService.listComments(workflowId));
    }

    @PostMapping("/{workflowId}/comments")
    @PreAuthorize("@securityService.canComment(#workflowId)")
    public ResponseEntity<WorkflowCommentDto> createComment(
            @PathVariable Long workflowId,
            @Valid @RequestBody CreateWorkflowCommentRequest request) {
        return ResponseEntity.ok(commentService.createComment(workflowId, request));
    }

    @PostMapping("/{workflowId}/comment-attachments")
    @PreAuthorize("@securityService.canComment(#workflowId)")
    public ResponseEntity<EntityAttachmentDto> uploadCommentAttachment(
            @PathVariable Long workflowId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(commentService.uploadCommentAttachmentDraft(workflowId, file));
    }
}
