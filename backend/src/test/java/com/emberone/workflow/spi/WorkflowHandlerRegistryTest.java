/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.workflow.exception.WorkflowConfigurationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowHandlerRegistryTest {

    @Mock WorkflowHandler<Object> firstHandler;
    @Mock WorkflowHandler<Object> secondHandler;

    @Test
    void registerIndexesHandlersByWorkflowTypeId() {
        when(firstHandler.workflowTypeId()).thenReturn(1L);
        when(secondHandler.workflowTypeId()).thenReturn(2L);
        WorkflowHandlerRegistry registry =
                new WorkflowHandlerRegistry(List.of(firstHandler, secondHandler));

        registry.register();

        assertThat(registry.require(1L)).isSameAs(firstHandler);
        assertThat(registry.require(2L)).isSameAs(secondHandler);
        assertThat(registry.all()).containsExactlyInAnyOrder(firstHandler, secondHandler);
    }

    @Test
    void registerRejectsDuplicateWorkflowTypeIds() {
        when(firstHandler.workflowTypeId()).thenReturn(1L);
        when(secondHandler.workflowTypeId()).thenReturn(1L);
        WorkflowHandlerRegistry registry =
                new WorkflowHandlerRegistry(List.of(firstHandler, secondHandler));

        assertThatThrownBy(registry::register)
                .isInstanceOf(WorkflowConfigurationException.class)
                .hasMessageContaining("Duplicate workflow handler registered");
    }

    @Test
    void requireThrowsForUnknownWorkflowType() {
        when(firstHandler.workflowTypeId()).thenReturn(1L);
        WorkflowHandlerRegistry registry = new WorkflowHandlerRegistry(List.of(firstHandler));
        registry.register();

        assertThatThrownBy(() -> registry.require(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Unknown workflow type id");
    }
}
