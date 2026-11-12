/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.service;

import com.emberone.platform.security.SecurityService;
import com.emberone.platform.security.TokenService;
import com.emberone.workflow.dto.WorkflowTypeCatalogDto;
import com.emberone.workflow.repository.WorkflowRepository;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
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
    private final TokenService tokenService;
    private final WorkflowRepository workflowRepository;

    public List<WorkflowTypeCatalogDto> listForCurrentUser() {
        Set<Long> activeTypeIds =
                workflowRepository.findActiveWorkflowTypeIdsByOpenedByUserId(
                        tokenService.requireUserId());
        return handlerRegistry.all().stream()
                .filter(handler ->
                        securityService.canBeginSelfServiceWorkflow(handler.workflowTypeId()))
                .map(handler -> toCatalogDto(handler, activeTypeIds))
                .sorted(
                        Comparator.comparing(WorkflowTypeCatalogDto::getOpenScope)
                                .thenComparing(WorkflowTypeCatalogDto::getWorkflowTypeId))
                .toList();
    }

    private static WorkflowTypeCatalogDto toCatalogDto(
            WorkflowHandler<?> handler, Set<Long> activeTypeIds) {
        return WorkflowTypeCatalogDto.builder()
                .workflowTypeId(handler.workflowTypeId())
                .workflowTypeKey(handler.workflowType().name())
                .mode(handler.mode())
                .allowsMultipleConcurrentOpen(handler.allowsMultipleConcurrentOpen())
                .openScope(handler.openScope())
                .listedInBeginnableCatalog(handler.listedInBeginnableCatalog())
                .typeBlocked(
                        !handler.allowsMultipleConcurrentOpen()
                                && activeTypeIds.contains(handler.workflowTypeId()))
                .build();
    }
}
