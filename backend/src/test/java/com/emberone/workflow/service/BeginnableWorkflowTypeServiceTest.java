/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.platform.security.SecurityService;
import com.emberone.workflow.domain.WorkflowMode;
import com.emberone.workflow.domain.WorkflowOpenScope;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.WorkflowTypeCatalogDto;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BeginnableWorkflowTypeServiceTest {

    @Mock WorkflowHandlerRegistry handlerRegistry;
    @Mock SecurityService securityService;
    @Mock WorkflowHandler<Object> allowedHandler;
    @Mock WorkflowHandler<Object> deniedHandler;

    @InjectMocks BeginnableWorkflowTypeService service;

    @Test
    void filtersBySecurityAndIncludesCatalogFlag() {
        when(allowedHandler.workflowType()).thenReturn(WorkflowType.NAME_CHANGE);
        when(allowedHandler.workflowTypeId()).thenReturn(WorkflowType.NAME_CHANGE.getId());
        when(allowedHandler.mode()).thenReturn(WorkflowMode.STATIC);
        when(allowedHandler.allowsMultipleConcurrentOpen()).thenReturn(false);
        when(allowedHandler.openScope()).thenReturn(WorkflowOpenScope.SELF);
        when(allowedHandler.listedInBeginnableCatalog()).thenReturn(false);
        when(deniedHandler.workflowTypeId()).thenReturn(WorkflowType.ADDRESS_CHANGE.getId());
        when(handlerRegistry.all()).thenReturn(List.of(allowedHandler, deniedHandler));
        when(securityService.canBeginSelfServiceWorkflow(WorkflowType.NAME_CHANGE.getId()))
                .thenReturn(true);
        when(securityService.canBeginSelfServiceWorkflow(WorkflowType.ADDRESS_CHANGE.getId()))
                .thenReturn(false);

        List<WorkflowTypeCatalogDto> result = service.listForCurrentUser();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getWorkflowTypeId())
                .isEqualTo(WorkflowType.NAME_CHANGE.getId());
        assertThat(result.getFirst().getWorkflowTypeKey()).isEqualTo("NAME_CHANGE");
        assertThat(result.getFirst().isListedInBeginnableCatalog()).isFalse();
    }
}
