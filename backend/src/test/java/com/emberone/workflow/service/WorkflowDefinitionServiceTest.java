/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.emberone.federalstate.FederalStateService;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.exception.WorkflowConfigurationException;
import com.emberone.workflow.repository.WorkflowDefinitionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowDefinitionServiceTest {

    @Mock WorkflowDefinitionRepository definitionRepository;
    @Mock TenantWorkflowDefinitionService tenantWorkflowDefinitionService;
    @Mock FederalStateService federalStateService;

    private WorkflowDefinitionService service;

    @BeforeEach
    void setUp() {
        service =
                new WorkflowDefinitionService(
                        definitionRepository, tenantWorkflowDefinitionService, federalStateService);
    }

    @Test
    void resolveEffectiveDefinitionReturnsOrgRow() {
        WorkflowDefinition orgRow =
                WorkflowDefinition.builder().id(2L).organizationId(50L).workflowTypeId(2L).build();
        when(definitionRepository.findByOrganizationIdAndWorkflowTypeId(50L, 2L))
                .thenReturn(Optional.of(orgRow));

        WorkflowDefinition result = service.resolveEffectiveDefinition(2L, 50L);

        assertThat(result).isSameAs(orgRow);
    }

    @Test
    void resolveEffectiveDefinitionThrowsWhenOrgRowMissing() {
        when(definitionRepository.findByOrganizationIdAndWorkflowTypeId(50L, 2L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveEffectiveDefinition(2L, 50L))
                .isInstanceOf(WorkflowConfigurationException.class)
                .hasMessageContaining("No workflow definition");
    }
}
