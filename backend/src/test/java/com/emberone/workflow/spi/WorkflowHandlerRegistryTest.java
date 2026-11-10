/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.workflow.exception.WorkflowConfigurationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowHandlerRegistryTest {

    @Mock WorkflowHandler<Object> firstHandler;
    @Mock WorkflowHandler<Object> secondHandler;

    @BeforeEach
    void declareContextRole() {
        lenient().when(firstHandler.approverSlotRoles()).thenReturn(List.of("CONTEXT"));
        lenient().when(secondHandler.approverSlotRoles()).thenReturn(List.of("CONTEXT"));
    }

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

    @Test
    void registerRejectsHandlerWithoutContextRole() {
        when(firstHandler.workflowTypeId()).thenReturn(1L);
        when(firstHandler.approverSlotRoles()).thenReturn(List.of("HOME_ORG"));
        WorkflowHandlerRegistry registry = new WorkflowHandlerRegistry(List.of(firstHandler));

        assertThatThrownBy(registry::register)
                .isInstanceOf(WorkflowConfigurationException.class)
                .hasMessageContaining("must declare the approver slot role CONTEXT");
    }

    @Test
    void registerRejectsHandlerDeclaringAdditionalOrARoleTwice() {
        when(firstHandler.workflowTypeId()).thenReturn(1L);
        when(firstHandler.approverSlotRoles()).thenReturn(List.of("CONTEXT", "ADDITIONAL"));
        assertThatThrownBy(() -> new WorkflowHandlerRegistry(List.of(firstHandler)).register())
                .isInstanceOf(WorkflowConfigurationException.class)
                .hasMessageContaining("must not declare ADDITIONAL");

        when(firstHandler.approverSlotRoles())
                .thenReturn(List.of("CONTEXT", "HOME_ORG", "HOME_ORG"));
        assertThatThrownBy(() -> new WorkflowHandlerRegistry(List.of(firstHandler)).register())
                .isInstanceOf(WorkflowConfigurationException.class)
                .hasMessageContaining("twice");
    }

    @Test
    void rolesOfListsHandlerRolesThenAdditional() {
        when(firstHandler.approverSlotRoles()).thenReturn(List.of("CONTEXT", "HOME_ORG"));

        assertThat(WorkflowApproverSlotRoles.rolesOf(firstHandler))
                .containsExactly("CONTEXT", "HOME_ORG", "ADDITIONAL");
        when(secondHandler.approverSlotRoles()).thenReturn(List.of("CONTEXT"));
        assertThat(WorkflowApproverSlotRoles.rolesOf(secondHandler))
                .containsExactly("CONTEXT", "ADDITIONAL");
    }
}
