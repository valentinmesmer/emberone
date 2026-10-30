/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.workflowtypecatalog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.workflow.domain.WorkflowMode;
import com.emberone.workflow.domain.WorkflowOpenScope;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.WorkflowTypeCatalogDto;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminWorkflowTypeCatalogControllerTest {

    @Mock WorkflowHandlerRegistry handlerRegistry;
    @Mock WorkflowHandler<Object> nameChangeHandler;
    @Mock WorkflowHandler<Object> addressChangeHandler;

    AdminWorkflowTypeCatalogController controller;

    @BeforeEach
    void setUp() {
        when(nameChangeHandler.workflowType()).thenReturn(WorkflowType.NAME_CHANGE);
        when(nameChangeHandler.workflowTypeId()).thenReturn(WorkflowType.NAME_CHANGE.getId());
        when(nameChangeHandler.mode()).thenReturn(WorkflowMode.STATIC);
        when(nameChangeHandler.allowsMultipleConcurrentOpen()).thenReturn(false);
        when(nameChangeHandler.openScope()).thenReturn(WorkflowOpenScope.SELF);
        when(nameChangeHandler.listedInBeginnableCatalog()).thenReturn(false);

        when(addressChangeHandler.workflowType()).thenReturn(WorkflowType.ADDRESS_CHANGE);
        when(addressChangeHandler.workflowTypeId()).thenReturn(WorkflowType.ADDRESS_CHANGE.getId());
        when(addressChangeHandler.mode()).thenReturn(WorkflowMode.DYNAMIC);
        when(addressChangeHandler.allowsMultipleConcurrentOpen()).thenReturn(true);
        when(addressChangeHandler.openScope()).thenReturn(WorkflowOpenScope.SELF);
        when(addressChangeHandler.listedInBeginnableCatalog()).thenReturn(false);

        when(handlerRegistry.all()).thenReturn(List.of(addressChangeHandler, nameChangeHandler));
        controller = new AdminWorkflowTypeCatalogController(handlerRegistry);
    }

    @Test
    void getCatalogReturnsSortedHandlerMetadata() {
        ResponseEntity<List<WorkflowTypeCatalogDto>> response = controller.getCatalog(1L);

        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getWorkflowTypeId())
                .isEqualTo(WorkflowType.NAME_CHANGE.getId());
        assertThat(response.getBody().get(1).getWorkflowTypeId())
                .isEqualTo(WorkflowType.ADDRESS_CHANGE.getId());
        assertThat(response.getBody().get(1).isAllowsMultipleConcurrentOpen()).isTrue();
        assertThat(response.getBody().get(0).getOpenScope()).isEqualTo(WorkflowOpenScope.SELF);
        assertThat(response.getBody().get(0).isListedInBeginnableCatalog()).isFalse();
    }
}
