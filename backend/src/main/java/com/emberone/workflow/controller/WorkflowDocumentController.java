/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.controller;

import com.emberone.attachment.dto.EntityAttachmentDto;
import com.emberone.attachment.service.EntityAttachmentService;
import com.emberone.platform.http.ContentDispositionHeaders;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.BoundedList;
import com.emberone.workflow.service.WorkflowDocumentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Upload (draft), list, and download of attachments linked to workflows. */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/workflows")
@RequiredArgsConstructor
public class WorkflowDocumentController {

    private final WorkflowDocumentService workflowDocumentService;
    private final TokenService tokenService;

    @PostMapping("/drafts/{workflowTypeId}/attachments")
    @PreAuthorize("@securityService.canUploadWorkflowDraftAttachment(#workflowTypeId)")
    public ResponseEntity<EntityAttachmentDto> uploadWorkflowDraftAttachment(
            @PathVariable long workflowTypeId, @RequestParam("file") MultipartFile file) {
        Long userId = tokenService.requireUserId();
        return ResponseEntity.ok(
                workflowDocumentService.uploadWorkflowAttachmentDraft(
                        userId, workflowTypeId, file));
    }

    @GetMapping("/{workflowId}/attachments")
    @BoundedList("Attachments on a single workflow instance")
    @PreAuthorize("@securityService.canDownloadWorkflowAttachment(#workflowId)")
    public ResponseEntity<List<EntityAttachmentDto>> listWorkflowAttachments(
            @PathVariable Long workflowId) {
        return ResponseEntity.ok(workflowDocumentService.listWorkflowAttachments(workflowId));
    }

    @GetMapping("/{workflowId}/attachments/{attachmentId}/download")
    @PreAuthorize("@securityService.canDownloadWorkflowAttachment(#workflowId)")
    public ResponseEntity<InputStreamResource> downloadWorkflowAttachment(
            @PathVariable Long workflowId, @PathVariable Long attachmentId) {
        log.debug("Downloading workflow attachment {} for workflow {}", attachmentId, workflowId);
        EntityAttachmentService.DownloadBundle bundle =
                workflowDocumentService.downloadWorkflowAttachment(workflowId, attachmentId);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDispositionHeaders.attachment(bundle.fileName()))
                .contentType(MediaType.parseMediaType(bundle.contentType()))
                .body(bundle.resource());
    }
}
