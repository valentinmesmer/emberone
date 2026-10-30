/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.workflowdefinition.controller;

import com.emberone.workflow.dto.TenantWorkflowDefinitionDto;
import com.emberone.workflow.dto.UpdateWorkflowDefinitionRequest;
import com.emberone.workflow.service.WorkflowDefinitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Tenant-wide workflow settings (Mandant Einstellungen). Standard-template approver routing lives
 * in the standards admin API.
 */
@RestController
@RequestMapping("/api/v1/auth/admin/tenants/{tenantId}/workflow-definitions")
@RequiredArgsConstructor
public class AdminWorkflowDefinitionController {

    private final WorkflowDefinitionService definitionService;

    @GetMapping("/{workflowTypeId}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantWorkflowDefinitionDto> getTenantDefault(
            @PathVariable long tenantId, @PathVariable Long workflowTypeId) {
        return ResponseEntity.ok(definitionService.getTenantDefault(tenantId, workflowTypeId));
    }

    @PutMapping("/{workflowTypeId}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantWorkflowDefinitionDto> updateTenantDefault(
            @PathVariable long tenantId,
            @PathVariable Long workflowTypeId,
            @Valid @RequestBody UpdateWorkflowDefinitionRequest request) {
        return ResponseEntity.ok(
                definitionService.updateTenantDefault(tenantId, workflowTypeId, request));
    }

    @PostMapping("/{workflowTypeId}/restore")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<TenantWorkflowDefinitionDto> restoreTenantDefault(
            @PathVariable long tenantId, @PathVariable Long workflowTypeId) {
        return ResponseEntity.ok(definitionService.restoreTenantDefault(tenantId, workflowTypeId));
    }
}
