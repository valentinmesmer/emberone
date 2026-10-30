/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.federalstate.FederalStateService;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.workflow.event.WorkflowConfigurationChangedEvent;
import com.emberone.workflow.repository.WorkflowDefinitionRepository;
import com.emberone.workflow.tenant.TenantWorkflowDefinition;
import com.emberone.workflow.tenant.TenantWorkflowDefinitionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TenantWorkflowDefinitionServiceTest {

    @Mock TenantWorkflowDefinitionRepository definitionRepository;
    @Mock WorkflowDefinitionRepository orgDefinitionRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock FederalStateService federalStateService;
    @Mock ApplicationEventPublisher eventPublisher;

    private TenantWorkflowDefinitionService service;

    @BeforeEach
    void setUp() {
        service =
                new TenantWorkflowDefinitionService(
                        definitionRepository,
                        orgDefinitionRepository,
                        organizationRepository,
                        federalStateService,
                        eventPublisher);
        TenantContextHolder.setTenantId(7L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void updateRejectsStaticApproverOrgOutsideTenant() {
        TenantWorkflowDefinition definition =
                TenantWorkflowDefinition.builder().id(1L).tenantId(7L).workflowTypeId(2L).build();
        when(definitionRepository.findByWorkflowTypeId(2L)).thenReturn(Optional.of(definition));
        when(organizationRepository.existsByIdAndTenantId(99L, 7L)).thenReturn(false);

        assertThatThrownBy(() -> service.update(7L, 2L, true, List.of(99L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to the tenant");
    }

    @Test
    void updateAcceptsStaticApproverOrgInTenant() {
        TenantWorkflowDefinition definition =
                TenantWorkflowDefinition.builder().id(1L).tenantId(7L).workflowTypeId(2L).build();
        when(definitionRepository.findByWorkflowTypeId(2L)).thenReturn(Optional.of(definition));
        when(organizationRepository.existsByIdAndTenantId(10L, 7L)).thenReturn(true);
        when(definitionRepository.save(definition)).thenReturn(definition);
        when(orgDefinitionRepository.findByWorkflowTypeId(2L)).thenReturn(List.of());

        service.update(7L, 2L, true, List.of(10L));

        assertThat(definition.getStaticOrganizations())
                .extracting(org -> org.getApproverOrganizationId())
                .containsExactly(10L);
        ArgumentCaptor<WorkflowConfigurationChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(WorkflowConfigurationChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().tenantId()).isEqualTo(7L);
        verify(definitionRepository).save(any(TenantWorkflowDefinition.class));
    }
}
