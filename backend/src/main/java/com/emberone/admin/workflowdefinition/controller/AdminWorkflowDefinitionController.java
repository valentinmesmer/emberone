/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.admin.workflowdefinition.controller;

import com.emberone.platform.web.list.BoundedList;
import com.emberone.workflow.dto.SaveWorkflowRoutingRuleRequest;
import com.emberone.workflow.dto.TenantWorkflowDefinitionDto;
import com.emberone.workflow.dto.UpdateWorkflowDefinitionRequest;
import com.emberone.workflow.dto.WorkflowRoutingRuleDto;
import com.emberone.workflow.service.WorkflowDefinitionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @GetMapping
    @BoundedList("Tenant default workflow definitions; one per workflow type")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<List<TenantWorkflowDefinitionDto>> listTenantDefaults(
            @PathVariable long tenantId) {
        return ResponseEntity.ok(definitionService.listTenantDefaults(tenantId));
    }

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

    @GetMapping("/{workflowTypeId}/routing-rules/{ruleId}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<WorkflowRoutingRuleDto> getRoutingRule(
            @PathVariable long tenantId,
            @PathVariable Long workflowTypeId,
            @PathVariable Long ruleId) {
        return ResponseEntity.ok(
                definitionService.getRoutingRule(tenantId, workflowTypeId, ruleId));
    }

    @PostMapping("/{workflowTypeId}/routing-rules")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<WorkflowRoutingRuleDto> createRoutingRule(
            @PathVariable long tenantId,
            @PathVariable Long workflowTypeId,
            @Valid @RequestBody SaveWorkflowRoutingRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(definitionService.createRoutingRule(tenantId, workflowTypeId, request));
    }

    @PutMapping("/{workflowTypeId}/routing-rules/{ruleId}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<WorkflowRoutingRuleDto> updateRoutingRule(
            @PathVariable long tenantId,
            @PathVariable Long workflowTypeId,
            @PathVariable Long ruleId,
            @Valid @RequestBody SaveWorkflowRoutingRuleRequest request) {
        return ResponseEntity.ok(
                definitionService.updateRoutingRule(tenantId, workflowTypeId, ruleId, request));
    }

    @DeleteMapping("/{workflowTypeId}/routing-rules/{ruleId}")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<Void> deleteRoutingRule(
            @PathVariable long tenantId,
            @PathVariable Long workflowTypeId,
            @PathVariable Long ruleId) {
        definitionService.deleteRoutingRule(tenantId, workflowTypeId, ruleId);
        return ResponseEntity.noContent().build();
    }
}
