/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.workflowtypecatalog.controller;

import com.emberone.platform.web.list.BoundedList;
import com.emberone.workflow.dto.WorkflowTypeCatalogDto;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/admin/tenants/{tenantId}/workflow-types")
@RequiredArgsConstructor
public class AdminWorkflowTypeCatalogController {

    private final WorkflowHandlerRegistry handlerRegistry;

    @GetMapping
    @BoundedList("Workflow type handler catalog")
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public ResponseEntity<List<WorkflowTypeCatalogDto>> getCatalog(@PathVariable long tenantId) {
        List<WorkflowTypeCatalogDto> catalog =
                handlerRegistry.all().stream()
                        .map(this::toCatalogDto)
                        .sorted(
                                Comparator.comparing(WorkflowTypeCatalogDto::getOpenScope)
                                        .thenComparing(WorkflowTypeCatalogDto::getWorkflowTypeId))
                        .toList();
        return ResponseEntity.ok(catalog);
    }

    private WorkflowTypeCatalogDto toCatalogDto(WorkflowHandler<?> handler) {
        return WorkflowTypeCatalogDto.builder()
                .workflowTypeId(handler.workflowTypeId())
                .workflowTypeKey(handler.workflowType().name())
                .mode(handler.mode())
                .allowsMultipleConcurrentOpen(handler.allowsMultipleConcurrentOpen())
                .openScope(handler.openScope())
                .listedInBeginnableCatalog(handler.listedInBeginnableCatalog())
                .build();
    }
}
