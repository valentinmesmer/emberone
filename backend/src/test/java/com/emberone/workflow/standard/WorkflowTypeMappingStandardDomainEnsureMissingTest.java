/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.standard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.federalstate.baseline.WorkflowTypeMappingBaseline;
import com.emberone.organization.domain.Organization;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.organizationgroup.standard.OrgGroupTemplateRepository;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.domain.StandardKind;
import com.emberone.standard.domain.StandardSource;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.domain.WorkflowTypeMapping;
import com.emberone.workflow.event.WorkflowConfigurationChangedEvent;
import com.emberone.workflow.repository.WorkflowDefinitionRepository;
import com.emberone.workflow.repository.WorkflowTypeMappingRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class WorkflowTypeMappingStandardDomainEnsureMissingTest {

    private static final long TENANT_ID = 1L;
    private static final long STANDARD_ID = 10L;

    @Mock WorkflowTypeMappingTemplateRepository templateRepository;
    @Mock WorkflowTypeMappingRepository mappingRepository;
    @Mock WorkflowDefinitionRepository definitionRepository;
    @Mock OrganizationGroupRepository organizationGroupRepository;
    @Mock OrgGroupTemplateRepository orgGroupTemplateRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    private WorkflowTypeMappingStandardDomain domain;

    @BeforeEach
    void setUp() {
        domain =
                new WorkflowTypeMappingStandardDomain(
                        templateRepository,
                        mappingRepository,
                        definitionRepository,
                        organizationGroupRepository,
                        orgGroupTemplateRepository,
                        eventPublisher);
    }

    @Test
    void ensureMissingFromBaselineRespectsBaselineEnabledFlag() {
        Standard standard =
                Standard.builder()
                        .id(STANDARD_ID)
                        .tenantId(TENANT_ID)
                        .kind(StandardKind.ORGANIZATION_SETUP)
                        .source(StandardSource.CUSTOM)
                        .name("Custom")
                        .build();
        WorkflowTypeMappingBaseline item =
                new WorkflowTypeMappingBaseline(
                        "5",
                        WorkflowType.TRAINING_COURSE_RECORD.getId(),
                        "hr-group",
                        false,
                        true,
                        false);

        when(templateRepository.findByStandardIdAndConfigKey(STANDARD_ID, "5"))
                .thenReturn(Optional.empty());
        when(orgGroupTemplateRepository.existsByStandardIdAndConfigKey(STANDARD_ID, "hr-group"))
                .thenReturn(true);
        when(templateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int added = domain.ensureMissingFromBaseline(standard, List.of(item));

        assertThat(added).isEqualTo(1);
        ArgumentCaptor<WorkflowTypeMappingTemplate> captor =
                ArgumentCaptor.forClass(WorkflowTypeMappingTemplate.class);
        verify(templateRepository).save(captor.capture());
        assertThat(captor.getValue().isEnabled()).isTrue();
        assertThat(captor.getValue().getWorkflowTypeId())
                .isEqualTo(WorkflowType.TRAINING_COURSE_RECORD.getId());
        assertThat(captor.getValue().getOrgGroupConfigKey()).isEqualTo("hr-group");
    }

    @Test
    void ensureMissingFromBaselineSkipsExisting() {
        Standard standard =
                Standard.builder()
                        .id(STANDARD_ID)
                        .tenantId(TENANT_ID)
                        .kind(StandardKind.ORGANIZATION_SETUP)
                        .source(StandardSource.CUSTOM)
                        .name("Custom")
                        .build();
        WorkflowTypeMappingBaseline item =
                new WorkflowTypeMappingBaseline(
                        "5",
                        WorkflowType.TRAINING_COURSE_RECORD.getId(),
                        "hr-group",
                        false,
                        true,
                        false);

        when(templateRepository.findByStandardIdAndConfigKey(STANDARD_ID, "5"))
                .thenReturn(
                        Optional.of(
                                WorkflowTypeMappingTemplate.builder()
                                        .configKey("5")
                                        .enabled(true)
                                        .build()));

        int added = domain.ensureMissingFromBaseline(standard, List.of(item));

        assertThat(added).isZero();
        verify(templateRepository, never()).save(any());
    }

    @Test
    void materializeForOrgCreatesDisabledDefinitionFlagsViaMapping() {
        // smoke: materialize still resolves approver group
        Organization org = Organization.builder().id(20L).tenantId(TENANT_ID).build();
        WorkflowTypeMappingTemplate template =
                WorkflowTypeMappingTemplate.builder()
                        .id(3L)
                        .tenantId(TENANT_ID)
                        .standardId(STANDARD_ID)
                        .configKey("4")
                        .workflowTypeId(WorkflowType.TRAINING_COURSE_RECORD.getId())
                        .orgGroupConfigKey("hr-group")
                        .enabled(false)
                        .build();
        when(templateRepository.findByStandardId(STANDARD_ID)).thenReturn(List.of(template));
        when(mappingRepository.findByOrganizationIdAndConfigKey(20L, "4"))
                .thenReturn(Optional.empty());
        when(organizationGroupRepository.findByOrganizationIdAndConfigKey(20L, "hr-group"))
                .thenReturn(
                        Optional.of(
                                OrganizationGroup.builder()
                                        .id(99L)
                                        .tenantId(TENANT_ID)
                                        .configKey("hr-group")
                                        .build()));
        when(mappingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(definitionRepository.findByOrganizationIdAndConfigKey(20L, "4"))
                .thenReturn(Optional.empty());

        domain.materializeForOrg(
                20L, Standard.builder().id(STANDARD_ID).tenantId(TENANT_ID).name("S").build());

        ArgumentCaptor<WorkflowTypeMapping> captor =
                ArgumentCaptor.forClass(WorkflowTypeMapping.class);
        verify(mappingRepository).save(captor.capture());
        assertThat(captor.getValue().getOrganizationGroupId()).isEqualTo(99L);
        assertThat(org.getId()).isEqualTo(20L);
    }

    @Test
    void reassignForOrgPublishesConfigurationChangedForAffectedWorkflowTypes() {
        Standard standard =
                Standard.builder().id(STANDARD_ID).tenantId(TENANT_ID).name("Custom").build();
        WorkflowTypeMappingTemplate template =
                WorkflowTypeMappingTemplate.builder()
                        .id(3L)
                        .tenantId(TENANT_ID)
                        .standardId(STANDARD_ID)
                        .configKey("5")
                        .workflowTypeId(WorkflowType.TRAINING_COURSE_RECORD.getId())
                        .orgGroupConfigKey("hr-group")
                        .build();
        WorkflowTypeMapping existing =
                WorkflowTypeMapping.builder()
                        .id(1L)
                        .tenantId(TENANT_ID)
                        .organizationId(20L)
                        .workflowTypeId(WorkflowType.TRAINING_COURSE_RECORD.getId())
                        .organizationGroupId(50L)
                        .configKey("5")
                        .standardOriginId(3L)
                        .build();

        when(templateRepository.findByStandardId(STANDARD_ID)).thenReturn(List.of(template));
        when(mappingRepository.findByOrganizationIdAndConfigKey(20L, "5"))
                .thenReturn(Optional.of(existing));
        when(organizationGroupRepository.findByOrganizationIdAndConfigKey(20L, "hr-group"))
                .thenReturn(
                        Optional.of(
                                OrganizationGroup.builder().id(99L).tenantId(TENANT_ID).build()));
        when(mappingRepository.findByOrganizationIdAndStandardOriginIdIsNotNull(20L))
                .thenReturn(List.of());
        when(definitionRepository.findByOrganizationIdAndConfigKey(20L, "5"))
                .thenReturn(Optional.empty());

        domain.reassignForOrg(20L, standard);

        ArgumentCaptor<WorkflowConfigurationChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(WorkflowConfigurationChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        WorkflowConfigurationChangedEvent event = eventCaptor.getValue();
        assertThat(event.tenantId()).isEqualTo(TENANT_ID);
        assertThat(event.workflowTypeId()).isEqualTo(WorkflowType.TRAINING_COURSE_RECORD.getId());
        assertThat(event.organizationIds()).containsExactly(20L);
    }
}
