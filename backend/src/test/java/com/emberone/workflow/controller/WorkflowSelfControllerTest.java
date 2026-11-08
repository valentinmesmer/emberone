/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.emberone.platform.web.list.ListQuery;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.WorkflowDto;
import com.emberone.workflow.dto.WorkflowOpenConcurrencyKeysDto;
import com.emberone.workflow.dto.WorkflowOpenStateDto;
import com.emberone.workflow.dto.WorkflowPageDto;
import com.emberone.workflow.dto.WorkflowTypeCatalogDto;
import com.emberone.workflow.list.WorkflowSelfListFilterOptions;
import com.emberone.workflow.service.BeginnableWorkflowTypeService;
import com.emberone.workflow.service.WorkflowService;
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
    @Mock BeginnableWorkflowTypeService beginnableWorkflowTypeService;
    @Mock WorkflowSelfListFilterOptions selfListFilterOptions;

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
    void getBeginnableTypesDelegatesToService() {
        List<WorkflowTypeCatalogDto> types =
                List.of(
                        WorkflowTypeCatalogDto.builder()
                                .workflowTypeId(WorkflowType.NAME_CHANGE.getId())
                                .workflowTypeKey("NAME_CHANGE")
                                .build());
        when(beginnableWorkflowTypeService.listForCurrentUser()).thenReturn(types);

        ResponseEntity<List<WorkflowTypeCatalogDto>> response = controller.getBeginnableTypes();

        assertThat(response.getBody()).isEqualTo(types);
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
