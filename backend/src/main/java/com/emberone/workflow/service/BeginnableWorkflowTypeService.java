/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.service;

import com.emberone.platform.security.SecurityService;
import com.emberone.workflow.dto.WorkflowTypeCatalogDto;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Self-service workflow types the current user may begin ({@code canBeginSelfServiceWorkflow}).
 * Single source for {@code GET /api/v1/self/workflows/beginnable-types} and the AI help chat, so
 * both always agree on what the user may start.
 */
@Service
@RequiredArgsConstructor
public class BeginnableWorkflowTypeService {

    private final WorkflowHandlerRegistry handlerRegistry;
    private final SecurityService securityService;

    public List<WorkflowTypeCatalogDto> listForCurrentUser() {
        return handlerRegistry.all().stream()
                .filter(handler ->
                        securityService.canBeginSelfServiceWorkflow(handler.workflowTypeId()))
                .map(BeginnableWorkflowTypeService::toCatalogDto)
                .sorted(
                        Comparator.comparing(WorkflowTypeCatalogDto::getOpenScope)
                                .thenComparing(WorkflowTypeCatalogDto::getWorkflowTypeId))
                .toList();
    }

    private static WorkflowTypeCatalogDto toCatalogDto(WorkflowHandler<?> handler) {
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
