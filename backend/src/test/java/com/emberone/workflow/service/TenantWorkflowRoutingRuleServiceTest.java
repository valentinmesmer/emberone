/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinitionRepository;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.exception.ValidationException;
import com.emberone.rank.tenant.TenantRankRepository;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import com.emberone.workflow.domain.WorkflowRoutingDimension;
import com.emberone.workflow.dto.SaveWorkflowRoutingRuleRequest;
import com.emberone.workflow.dto.WorkflowRoutingRuleDto;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import com.emberone.workflow.tenant.TenantWorkflowDefinition;
import com.emberone.workflow.tenant.TenantWorkflowRoutingRule;
import com.emberone.workflow.tenant.TenantWorkflowRoutingRuleOrg;
import com.emberone.workflow.tenant.TenantWorkflowRoutingRuleRepository;
import com.emberone.workflow.tenant.TenantWorkflowRoutingRuleSubject;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantWorkflowRoutingRuleServiceTest {

    private static final long TENANT_ID = 7L;
    private static final long TYPE_ID = 3L;
    private static final long DEFINITION_ID = 1L;

    @Mock TenantWorkflowRoutingRuleRepository ruleRepository;
    @Mock WorkflowHandlerRegistry handlerRegistry;
    @Mock OrganizationRepository organizationRepository;
    @Mock TenantCertDefinitionRepository certDefinitionRepository;
    @Mock TenantTrainingCourseTypeRepository trainingCourseTypeRepository;
    @Mock TenantDrivingLicenseDefinitionRepository drivingLicenseDefinitionRepository;
    @Mock TenantRankRepository rankRepository;
    @Mock WorkflowHandler<Object> handler;

    private TenantWorkflowRoutingRuleService service;
    private TenantWorkflowDefinition definition;

    @BeforeEach
    void setUp() {
        service =
                new TenantWorkflowRoutingRuleService(
                        ruleRepository,
                        handlerRegistry,
                        organizationRepository,
                        certDefinitionRepository,
                        trainingCourseTypeRepository,
                        drivingLicenseDefinitionRepository,
                        rankRepository);
        definition =
                TenantWorkflowDefinition.builder()
                        .id(DEFINITION_ID)
                        .tenantId(TENANT_ID)
                        .workflowTypeId(TYPE_ID)
                        .build();
    }

    private void dimension(WorkflowRoutingDimension dimension) {
        org.mockito.Mockito.doReturn(handler).when(handlerRegistry).require(TYPE_ID);
        when(handler.routingDimension()).thenReturn(Optional.ofNullable(dimension));
    }

    private static SaveWorkflowRoutingRuleRequest request(
            List<Long> subjectIds, List<Long> orgIds) {
        return SaveWorkflowRoutingRuleRequest.builder()
                .subjectIds(subjectIds)
                .staticApproverOrganizationIds(orgIds)
                .build();
    }

    @Test
    void createPersistsSubjectsAndOrgsOfTheRule() {
        dimension(WorkflowRoutingDimension.CERTIFICATION_DEFINITION);
        when(certDefinitionRepository.existsById(100L)).thenReturn(true);
        when(certDefinitionRepository.existsById(101L)).thenReturn(true);
        when(organizationRepository.existsByIdAndTenantId(20L, TENANT_ID)).thenReturn(true);

        WorkflowRoutingRuleDto created =
                service.create(definition, request(List.of(100L, 101L, 100L), List.of(20L)));

        ArgumentCaptor<TenantWorkflowRoutingRule> captor =
                ArgumentCaptor.forClass(TenantWorkflowRoutingRule.class);
        verify(ruleRepository).save(captor.capture());
        TenantWorkflowRoutingRule saved = captor.getValue();
        assertThat(created.getSubjectIds()).containsExactly(100L, 101L);
        assertThat(created.getStaticApproverOrganizationIds()).containsExactly(20L);
        assertThat(saved.getSubjects())
                .extracting(TenantWorkflowRoutingRuleSubject::getSubjectId)
                .containsExactly(100L, 101L);
        assertThat(saved.getSubjects()).allMatch(s -> s.getDefinitionId() == DEFINITION_ID);
        assertThat(saved.getOrganizations())
                .extracting(TenantWorkflowRoutingRuleOrg::getApproverOrganizationId)
                .containsExactly(20L);
    }

    @Test
    void createRejectsTypeWithoutRoutingDimension() {
        dimension(null);

        assertThatThrownBy(() -> service.create(definition, request(List.of(1L), List.of())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("cannot be split");
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void createRejectsSubjectOutsideTheTenantCatalog() {
        dimension(WorkflowRoutingDimension.TRAINING_COURSE_TYPE);
        when(trainingCourseTypeRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(definition, request(List.of(999L), List.of())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("999");
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void createChecksTheCatalogOfTheDimension() {
        dimension(WorkflowRoutingDimension.TRAINING_COURSE_TYPE);
        when(trainingCourseTypeRepository.existsById(5L)).thenReturn(true);

        service.create(definition, request(List.of(5L), List.of()));

        verify(trainingCourseTypeRepository).existsById(5L);
        verify(certDefinitionRepository, never()).existsById(any());
    }

    @Test
    void createRejectsSubjectAlreadyUsedByAnotherRule() {
        dimension(WorkflowRoutingDimension.CERTIFICATION_DEFINITION);
        when(certDefinitionRepository.existsById(100L)).thenReturn(true);
        TenantWorkflowRoutingRule other = existingRule(50L, 100L);
        when(ruleRepository.findByDefinitionIdOrderByIdAsc(DEFINITION_ID))
                .thenReturn(List.of(other));

        assertThatThrownBy(() -> service.create(definition, request(List.of(100L), List.of())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already used by another rule");
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void updateMayKeepItsOwnSubjects() {
        dimension(WorkflowRoutingDimension.CERTIFICATION_DEFINITION);
        when(certDefinitionRepository.existsById(100L)).thenReturn(true);
        when(certDefinitionRepository.existsById(102L)).thenReturn(true);
        TenantWorkflowRoutingRule own = existingRule(50L, 100L);
        when(ruleRepository.findByIdAndDefinitionId(50L, DEFINITION_ID))
                .thenReturn(Optional.of(own));
        when(ruleRepository.findByDefinitionIdOrderByIdAsc(DEFINITION_ID)).thenReturn(List.of(own));

        service.update(definition, 50L, request(List.of(100L, 102L), List.of()));

        assertThat(own.getSubjects())
                .extracting(TenantWorkflowRoutingRuleSubject::getSubjectId)
                .containsExactlyInAnyOrder(100L, 102L);
    }

    @Test
    void createRejectsOrganizationOutsideTheTenant() {
        dimension(WorkflowRoutingDimension.CERTIFICATION_DEFINITION);
        when(certDefinitionRepository.existsById(100L)).thenReturn(true);
        when(organizationRepository.existsByIdAndTenantId(99L, TENANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.create(definition, request(List.of(100L), List.of(99L))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not belong to the tenant");
    }

    @Test
    void updateAndDeleteRejectUnknownRule() {
        when(ruleRepository.findByIdAndDefinitionId(77L, DEFINITION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(definition, 77L))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.update(definition, 77L, request(List.of(1L), List.of())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getReturnsTheRuleOfTheDefinition() {
        TenantWorkflowRoutingRule own = existingRule(50L, 102L, 100L);
        when(ruleRepository.findByIdAndDefinitionId(50L, DEFINITION_ID))
                .thenReturn(Optional.of(own));

        WorkflowRoutingRuleDto dto = service.get(definition, 50L);

        assertThat(dto.getId()).isEqualTo(50L);
        assertThat(dto.getSubjectIds()).containsExactly(100L, 102L);
    }

    @Test
    void getRejectsUnknownOrForeignRule() {
        when(ruleRepository.findByIdAndDefinitionId(77L, DEFINITION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(definition, 77L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesTheRule() {
        TenantWorkflowRoutingRule own = existingRule(50L, 100L);
        when(ruleRepository.findByIdAndDefinitionId(50L, DEFINITION_ID))
                .thenReturn(Optional.of(own));

        service.delete(definition, 50L);

        verify(ruleRepository).delete(own);
    }

    @Test
    void listMapsRulesSorted() {
        TenantWorkflowRoutingRule rule = existingRule(50L, 102L, 100L);
        when(ruleRepository.findByDefinitionIdOrderByIdAsc(DEFINITION_ID))
                .thenReturn(List.of(rule));

        List<WorkflowRoutingRuleDto> rules = service.list(definition);

        assertThat(rules)
                .singleElement()
                .satisfies(dto -> {
                    assertThat(dto.getId()).isEqualTo(50L);
                    assertThat(dto.getSubjectIds()).containsExactly(100L, 102L);
                });
    }

    private TenantWorkflowRoutingRule existingRule(long id, Long... subjectIds) {
        TenantWorkflowRoutingRule rule =
                TenantWorkflowRoutingRule.builder()
                        .id(id)
                        .tenantId(TENANT_ID)
                        .definition(definition)
                        .build();
        for (Long subjectId : subjectIds) {
            rule.getSubjects()
                    .add(
                            TenantWorkflowRoutingRuleSubject.builder()
                                    .tenantId(TENANT_ID)
                                    .definitionId(DEFINITION_ID)
                                    .rule(rule)
                                    .subjectId(subjectId)
                                    .build());
        }
        return rule;
    }
}
