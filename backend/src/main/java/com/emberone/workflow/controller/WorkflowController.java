/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.controller;

import com.emberone.workflow.dto.*;
import com.emberone.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping("/{id}")
    @PreAuthorize("@securityService.canView(#id)")
    public ResponseEntity<WorkflowDto> getWorkflow(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getWorkflow(id));
    }

    @GetMapping("/{id}/document-promotion-preview")
    @PreAuthorize("@securityService.canView(#id)")
    public ResponseEntity<WorkflowDocumentPromotionPreviewDto> getDocumentPromotionPreview(
            @PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getDocumentPromotionPreview(id));
    }

    @PutMapping("/{id}/document-promotions")
    @PreAuthorize("@securityService.canUpdateDocumentPromotions(#id)")
    public ResponseEntity<WorkflowDocumentPromotionPreviewDto> updateDocumentPromotions(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkflowDocumentPromotionsRequest request) {
        return ResponseEntity.ok(workflowService.updateDocumentPromotionSelections(id, request));
    }

    @PutMapping("/{id}/payload")
    @PreAuthorize("@securityService.canUpdatePayload(#id)")
    public ResponseEntity<WorkflowDto> updatePayload(
            @PathVariable Long id, @RequestBody Object payload) {
        return ResponseEntity.ok(workflowService.updatePayload(id, payload));
    }

    @PostMapping("/{id}/input-change-request")
    @PreAuthorize("@securityService.canRequestInputChange(#id)")
    public ResponseEntity<WorkflowDto> requestInputChange(
            @PathVariable Long id, @Valid @RequestBody RequestWorkflowInputChangeRequest request) {
        return ResponseEntity.ok(workflowService.requestInputChange(id, request));
    }

    @PostMapping("/{id}/input-change-request/withdraw")
    @PreAuthorize("@securityService.canWithdrawInputChange(#id)")
    public ResponseEntity<WorkflowDto> withdrawInputChange(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.withdrawInputChange(id));
    }

    @PostMapping("/{id}/participants/{participantId}/claim")
    @PreAuthorize("@securityService.canClaim(#id, #participantId)")
    public ResponseEntity<WorkflowDto> claim(
            @PathVariable Long id, @PathVariable Long participantId) {
        return ResponseEntity.ok(workflowService.claim(id, participantId));
    }

    @PostMapping("/{id}/participants/{participantId}/unclaim")
    @PreAuthorize("@securityService.canUnclaim(#id, #participantId)")
    public ResponseEntity<WorkflowDto> unclaim(
            @PathVariable Long id, @PathVariable Long participantId) {
        return ResponseEntity.ok(workflowService.unclaim(id, participantId));
    }

    @PostMapping("/{id}/participants/{participantId}/withdraw-approval")
    @PreAuthorize("@securityService.canWithdrawApproval(#id, #participantId)")
    public ResponseEntity<WorkflowDto> withdrawApproval(
            @PathVariable Long id, @PathVariable Long participantId) {
        return ResponseEntity.ok(workflowService.withdrawApproval(id, participantId));
    }

    @PostMapping("/{id}/decisions")
    @PreAuthorize("@securityService.canDecide(#id, #request.participantId)")
    public ResponseEntity<WorkflowDto> decide(
            @PathVariable Long id, @Valid @RequestBody WorkflowDecisionRequest request) {
        return ResponseEntity.ok(workflowService.decide(id, request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@securityService.canCancel(#id)")
    public ResponseEntity<WorkflowDto> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.cancel(id));
    }
}
