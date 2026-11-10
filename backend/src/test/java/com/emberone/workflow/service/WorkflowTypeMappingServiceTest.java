/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.emberone.workflow.domain.WorkflowTypeMapping;
import com.emberone.workflow.exception.WorkflowConfigurationException;
import com.emberone.workflow.repository.WorkflowTypeMappingRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowTypeMappingServiceTest {

    @Mock WorkflowTypeMappingRepository mappingRepository;

    private WorkflowTypeMappingService service;

    @BeforeEach
    void setUp() {
        service = new WorkflowTypeMappingService(mappingRepository);
    }

    @Test
    void requireMappingThrowsWhenMissing() {
        when(mappingRepository.findByOrganizationIdAndWorkflowTypeId(5L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireMapping(5L, 1L))
                .isInstanceOf(WorkflowConfigurationException.class)
                .hasMessageContaining("Missing workflow type mapping");
    }

    @Test
    void requireSlotFailsClosedWhenTheRoleHasNoRow() {
        WorkflowTypeMapping mapping = mock(WorkflowTypeMapping.class);
        when(mapping.getId()).thenReturn(9L);
        when(mapping.findSlot("HOME_ORG")).thenReturn(Optional.empty());
        when(mappingRepository.findByOrganizationIdAndWorkflowTypeId(5L, 17L))
                .thenReturn(Optional.of(mapping));

        assertThatThrownBy(() -> service.requireSlot(5L, 17L, "HOME_ORG"))
                .isInstanceOf(WorkflowConfigurationException.class)
                .hasMessageContaining("no approver slot settings for role HOME_ORG");
        verify(mapping, never()).findSlot("CONTEXT");
    }
}
