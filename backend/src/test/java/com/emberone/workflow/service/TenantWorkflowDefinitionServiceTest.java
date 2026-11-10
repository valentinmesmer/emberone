/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.federalstate.FederalStateService;
import com.emberone.federalstate.baseline.WorkflowDefinitionBaseline;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.workflow.dto.SaveWorkflowRoutingRuleRequest;
import com.emberone.workflow.event.WorkflowConfigurationChangedEvent;
import com.emberone.workflow.event.WorkflowConfigurationScope;
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
    @Mock TenantWorkflowRoutingRuleService routingRuleService;
    @Mock OrganizationRepository organizationRepository;
    @Mock FederalStateService federalStateService;
    @Mock ApplicationEventPublisher eventPublisher;

    private TenantWorkflowDefinitionService service;

    @BeforeEach
    void setUp() {
        service =
                new TenantWorkflowDefinitionService(
                        definitionRepository,
                        routingRuleService,
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

    @Test
    void restoreDeletesAllRoutingRules() {
        TenantWorkflowDefinition definition =
                TenantWorkflowDefinition.builder()
                        .id(1L)
                        .tenantId(7L)
                        .workflowTypeId(2L)
                        .customized(true)
                        .build();
        when(definitionRepository.findByWorkflowTypeId(2L)).thenReturn(Optional.of(definition));
        when(definitionRepository.save(definition)).thenReturn(definition);

        service.restore(7L, 2L, new WorkflowDefinitionBaseline("2", 2L, false));

        verify(routingRuleService).deleteAll(definition);
        assertThat(definition.isCustomized()).isFalse();
        verify(eventPublisher).publishEvent(any(WorkflowConfigurationChangedEvent.class));
    }

    @Test
    void routingRuleChangesPublishTenantWideConfigurationChange() {
        TenantWorkflowDefinition definition =
                TenantWorkflowDefinition.builder().id(1L).tenantId(7L).workflowTypeId(2L).build();
        when(definitionRepository.findByWorkflowTypeId(2L)).thenReturn(Optional.of(definition));
        when(definitionRepository.save(definition)).thenReturn(definition);
        SaveWorkflowRoutingRuleRequest request =
                SaveWorkflowRoutingRuleRequest.builder().subjectIds(List.of(5L)).build();

        service.createRoutingRule(7L, 2L, request);
        service.updateRoutingRule(7L, 2L, 9L, request);
        service.deleteRoutingRule(7L, 2L, 9L);

        verify(routingRuleService).create(definition, request);
        verify(routingRuleService).update(definition, 9L, request);
        verify(routingRuleService).delete(definition, 9L);
        ArgumentCaptor<WorkflowConfigurationChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(WorkflowConfigurationChangedEvent.class);
        verify(eventPublisher, times(3)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .allSatisfy(event -> {
                    assertThat(event.tenantId()).isEqualTo(7L);
                    assertThat(event.workflowTypeId()).isEqualTo(2L);
                    assertThat(event.scope()).isEqualTo(WorkflowConfigurationScope.TENANT_WIDE);
                });
        assertThat(definition.isCustomized()).isTrue();
    }
}
