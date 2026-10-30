/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.controller;

import com.emberone.platform.security.SecurityService;
import com.emberone.platform.web.list.BoundedList;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.workflow.dto.WorkflowOpenConcurrencyKeysDto;
import com.emberone.workflow.dto.WorkflowOpenStateDto;
import com.emberone.workflow.dto.WorkflowPageDto;
import com.emberone.workflow.dto.WorkflowTypeCatalogDto;
import com.emberone.workflow.list.WorkflowListFilter;
import com.emberone.workflow.list.WorkflowSelfListFilterOptions;
import com.emberone.workflow.service.WorkflowService;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lists workflows for the <strong>currently authenticated user</strong> only. User id (and tenant)
 * come from {@link com.emberone.platform.security.TokenService} inside {@link WorkflowService} - no
 * user id in the URL. Same URL prefix as other self controllers (e.g. {@code
 * /api/v1/self/time-entries}).
 */
@RestController
@RequestMapping("/api/v1/self/workflows")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class WorkflowSelfController {

    private final WorkflowService workflowService;
    private final WorkflowHandlerRegistry handlerRegistry;
    private final SecurityService workflowSecurityService;
    private final WorkflowSelfListFilterOptions selfListFilterOptions;

    @GetMapping("/list-filter-options")
    public ResponseEntity<ListFilterOptionsDto> listFilterOptions() {
        return ResponseEntity.ok(selfListFilterOptions.filterOptions());
    }

    @GetMapping("/opened")
    public ResponseEntity<WorkflowPageDto> listOpened(
            ListQuery query,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<String> type) {
        return ResponseEntity.ok(
                workflowService.listOpenedForCurrentUser(
                        query, WorkflowListFilter.parse(status, type)));
    }

    @GetMapping("/participations")
    public ResponseEntity<WorkflowPageDto> listParticipations(
            ListQuery query,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<String> type) {
        return ResponseEntity.ok(
                workflowService.listParticipationsForCurrentUser(
                        query, WorkflowListFilter.parse(status, type)));
    }

    @GetMapping("/pending-count")
    public ResponseEntity<Map<String, Long>> getPendingCount() {
        return ResponseEntity.ok(Map.of("count", workflowService.countPendingForCurrentUser()));
    }

    /**
     * Self-service workflow types the current user may begin (open or use prep endpoints such as
     * proof upload). Includes dedicated-entry types ({@code listedInBeginnableCatalog=false}); the
     * Anträge picker shows only catalog-listed types.
     */
    @GetMapping("/beginnable-types")
    @BoundedList("Self-service beginnable workflow type catalog")
    public ResponseEntity<List<WorkflowTypeCatalogDto>> getBeginnableTypes() {
        List<WorkflowTypeCatalogDto> types =
                handlerRegistry.all().stream()
                        .filter(
                                h ->
                                        workflowSecurityService.canBeginSelfServiceWorkflow(
                                                h.workflowTypeId()))
                        .map(this::toCatalogDto)
                        .sorted(
                                Comparator.comparing(WorkflowTypeCatalogDto::getOpenScope)
                                        .thenComparing(WorkflowTypeCatalogDto::getWorkflowTypeId))
                        .toList();
        return ResponseEntity.ok(types);
    }

    /** Active concurrency keys the current user already holds for one or all workflow types. */
    @GetMapping("/open-concurrency-keys")
    @BoundedList("Open workflow concurrency keys for current user")
    public ResponseEntity<List<WorkflowOpenConcurrencyKeysDto>> getOpenConcurrencyKeys(
            @RequestParam(required = false) Long workflowTypeId) {
        return ResponseEntity.ok(
                workflowService.getOpenConcurrencyKeysForCurrentUser(workflowTypeId));
    }

    /**
     * Whether the current user may open another workflow of this type and which concurrency keys
     * are already taken.
     */
    @GetMapping("/open-state/{workflowTypeId}")
    public ResponseEntity<WorkflowOpenStateDto> getOpenState(@PathVariable long workflowTypeId) {
        return ResponseEntity.ok(workflowService.getOpenStateForCurrentUser(workflowTypeId));
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
