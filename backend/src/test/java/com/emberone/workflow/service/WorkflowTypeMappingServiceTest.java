/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

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
}
