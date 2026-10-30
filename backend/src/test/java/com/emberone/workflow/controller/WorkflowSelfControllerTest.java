/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.emberone.platform.security.SecurityService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.workflow.domain.WorkflowMode;
import com.emberone.workflow.domain.WorkflowOpenScope;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.WorkflowDto;
import com.emberone.workflow.dto.WorkflowOpenConcurrencyKeysDto;
import com.emberone.workflow.dto.WorkflowOpenStateDto;
import com.emberone.workflow.dto.WorkflowPageDto;
import com.emberone.workflow.dto.WorkflowTypeCatalogDto;
import com.emberone.workflow.list.WorkflowSelfListFilterOptions;
import com.emberone.workflow.service.WorkflowService;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class WorkflowSelfControllerTest {

    @Mock WorkflowService workflowService;
    @Mock WorkflowHandlerRegistry handlerRegistry;
    @Mock SecurityService workflowSecurityService;
    @Mock WorkflowSelfListFilterOptions selfListFilterOptions;
    @Mock WorkflowHandler<Object> allowedHandler;
    @Mock WorkflowHandler<Object> deniedHandler;

    @InjectMocks WorkflowSelfController controller;

    @Test
    void listOpenedDelegatesToService() {
        WorkflowPageDto page =
                WorkflowPageDto.builder()
                        .content(List.of(WorkflowDto.builder().id(1L).build()))
                        .page(0)
                        .size(50)
                        .totalElements(1)
                        .totalPages(1)
                        .hasNext(false)
                        .build();
        when(workflowService.listOpenedForCurrentUser(any(), any())).thenReturn(page);

        ResponseEntity<WorkflowPageDto> response =
                controller.listOpened(new ListQuery(0, 50, null, null), null, null);

        assertThat(response.getBody()).isEqualTo(page);
    }

    @Test
    void getBeginnableTypesFiltersBySecurityAndIncludesCatalogFlag() {
        when(allowedHandler.workflowType()).thenReturn(WorkflowType.NAME_CHANGE);
        when(allowedHandler.workflowTypeId()).thenReturn(WorkflowType.NAME_CHANGE.getId());
        when(allowedHandler.mode()).thenReturn(WorkflowMode.STATIC);
        when(allowedHandler.allowsMultipleConcurrentOpen()).thenReturn(false);
        when(allowedHandler.openScope()).thenReturn(WorkflowOpenScope.SELF);
        when(allowedHandler.listedInBeginnableCatalog()).thenReturn(false);
        when(deniedHandler.workflowTypeId()).thenReturn(WorkflowType.ADDRESS_CHANGE.getId());
        when(handlerRegistry.all()).thenReturn(List.of(allowedHandler, deniedHandler));
        when(workflowSecurityService.canBeginSelfServiceWorkflow(WorkflowType.NAME_CHANGE.getId()))
                .thenReturn(true);
        when(workflowSecurityService.canBeginSelfServiceWorkflow(
                        WorkflowType.ADDRESS_CHANGE.getId()))
                .thenReturn(false);

        ResponseEntity<List<WorkflowTypeCatalogDto>> response = controller.getBeginnableTypes();

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().getWorkflowTypeId())
                .isEqualTo(WorkflowType.NAME_CHANGE.getId());
        assertThat(response.getBody().getFirst().getWorkflowTypeKey()).isEqualTo("NAME_CHANGE");
        assertThat(response.getBody().getFirst().isListedInBeginnableCatalog()).isFalse();
    }

    @Test
    void getOpenStateDelegatesToService() {
        WorkflowOpenStateDto state =
                WorkflowOpenStateDto.builder()
                        .workflowTypeId(WorkflowType.ORGANIZATION_JOIN_REQUEST.getId())
                        .typeBlocked(false)
                        .openConcurrencyKeys(List.of("org:5"))
                        .build();
        when(workflowService.getOpenStateForCurrentUser(
                        WorkflowType.ORGANIZATION_JOIN_REQUEST.getId()))
                .thenReturn(state);

        ResponseEntity<WorkflowOpenStateDto> response =
                controller.getOpenState(WorkflowType.ORGANIZATION_JOIN_REQUEST.getId());

        assertThat(response.getBody()).isEqualTo(state);
    }

    @Test
    void getOpenConcurrencyKeysDelegatesToService() {
        List<WorkflowOpenConcurrencyKeysDto> keys =
                List.of(
                        WorkflowOpenConcurrencyKeysDto.builder()
                                .workflowTypeId(WorkflowType.ORGANIZATION_JOIN_REQUEST.getId())
                                .keys(List.of("org:5"))
                                .build());
        when(workflowService.getOpenConcurrencyKeysForCurrentUser(
                        WorkflowType.ORGANIZATION_JOIN_REQUEST.getId()))
                .thenReturn(keys);

        ResponseEntity<List<WorkflowOpenConcurrencyKeysDto>> response =
                controller.getOpenConcurrencyKeys(WorkflowType.ORGANIZATION_JOIN_REQUEST.getId());

        assertThat(response.getBody()).isEqualTo(keys);
    }
}
