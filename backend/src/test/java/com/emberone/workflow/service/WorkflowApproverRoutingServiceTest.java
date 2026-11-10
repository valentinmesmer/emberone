/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.emberone.workflow.service.WorkflowApproverRoutingService.Routing;
import com.emberone.workflow.tenant.TenantWorkflowDefinition;
import com.emberone.workflow.tenant.TenantWorkflowDefinitionRepository;
import com.emberone.workflow.tenant.TenantWorkflowDefinitionStaticOrg;
import com.emberone.workflow.tenant.TenantWorkflowRoutingRule;
import com.emberone.workflow.tenant.TenantWorkflowRoutingRuleOrg;
import com.emberone.workflow.tenant.TenantWorkflowRoutingRuleRepository;
import com.emberone.workflow.tenant.TenantWorkflowRoutingRuleSubject;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowApproverRoutingServiceTest {

    private static final long TYPE_ID = 3L;
    private static final long DEFINITION_ID = 1L;

    @Mock TenantWorkflowDefinitionRepository definitionRepository;
    @Mock TenantWorkflowRoutingRuleRepository ruleRepository;

    private WorkflowApproverRoutingService service;
    private TenantWorkflowDefinition definition;

    @BeforeEach
    void setUp() {
        service = new WorkflowApproverRoutingService(definitionRepository, ruleRepository);
        definition =
                TenantWorkflowDefinition.builder()
                        .id(DEFINITION_ID)
                        .tenantId(7L)
                        .workflowTypeId(TYPE_ID)
                        .build();
        lenient()
                .when(definitionRepository.findByWorkflowTypeId(TYPE_ID))
                .thenReturn(Optional.of(definition));
    }

    @Test
    void withoutSubjectsTheDefaultRuleApplies() {
        defaultRule(false, 10L, 11L);
        rule(1L, List.of(100L), List.of(20L));

        Routing routing = service.resolve(TYPE_ID, Set.of());

        assertThat(routing.staticOrganizationIds()).containsExactly(10L, 11L);
        assertThat(routing.staticApproversExclusive()).isFalse();
    }

    @Test
    void subjectWithRuleUsesItsRuleInsteadOfTheDefault() {
        defaultRule(false, 10L);
        TenantWorkflowRoutingRule rule = rule(1L, List.of(100L), List.of(20L));
        when(ruleRepository.findByDefinitionIdOrderByIdAsc(DEFINITION_ID))
                .thenReturn(List.of(rule));

        Routing routing = service.resolve(TYPE_ID, Set.of(100L));

        assertThat(routing.staticOrganizationIds()).containsExactly(20L);
        assertThat(routing.staticApproversExclusive()).isFalse();
    }

    @Test
    void subjectWithoutRuleFallsBackToTheDefaultRule() {
        defaultRule(false, 10L);
        TenantWorkflowRoutingRule rule = rule(1L, List.of(100L), List.of(20L));
        when(ruleRepository.findByDefinitionIdOrderByIdAsc(DEFINITION_ID))
                .thenReturn(List.of(rule));

        Routing routing = service.resolve(TYPE_ID, Set.of(999L));

        assertThat(routing.staticOrganizationIds()).containsExactly(10L);
    }

    @Test
    void multipleSubjectsUnionTheOrgsWithoutDuplicates() {
        defaultRule(false, 10L, 22L);
        TenantWorkflowRoutingRule ruleA = rule(1L, List.of(100L), List.of(20L, 21L));
        TenantWorkflowRoutingRule ruleB = rule(2L, List.of(101L), List.of(21L, 22L));
        when(ruleRepository.findByDefinitionIdOrderByIdAsc(DEFINITION_ID))
                .thenReturn(List.of(ruleA, ruleB));

        Routing routing = service.resolve(TYPE_ID, List.of(100L, 101L, 999L));

        assertThat(routing.staticOrganizationIds()).containsExactlyInAnyOrder(10L, 20L, 21L, 22L);
        assertThat(routing.staticOrganizationIds()).doesNotHaveDuplicates();
    }

    @Test
    void sameRuleForSeveralSubjectsCountsOnce() {
        defaultRule(false);
        TenantWorkflowRoutingRule rule = rule(1L, List.of(100L, 101L), List.of(20L));
        when(ruleRepository.findByDefinitionIdOrderByIdAsc(DEFINITION_ID))
                .thenReturn(List.of(rule));

        Routing routing = service.resolve(TYPE_ID, Set.of(100L, 101L));

        assertThat(routing.staticOrganizationIds()).containsExactly(20L);
    }

    @Test
    void exclusiveIsTheWorkflowWideSwitchForRuleOrgsToo() {
        defaultRule(true, 10L);
        TenantWorkflowRoutingRule rule = rule(1L, List.of(100L), List.of(20L));
        when(ruleRepository.findByDefinitionIdOrderByIdAsc(DEFINITION_ID))
                .thenReturn(List.of(rule));

        assertThat(service.resolve(TYPE_ID, Set.of(100L)).staticApproversExclusive()).isTrue();
        assertThat(service.resolve(TYPE_ID, List.of(100L, 999L)).staticApproversExclusive())
                .isTrue();
        assertThat(service.resolve(TYPE_ID, Set.of()).staticApproversExclusive()).isTrue();
    }

    @Test
    void notExclusiveWhenTheWorkflowSwitchIsOff() {
        defaultRule(false, 10L);
        TenantWorkflowRoutingRule rule = rule(1L, List.of(100L), List.of(20L));
        when(ruleRepository.findByDefinitionIdOrderByIdAsc(DEFINITION_ID))
                .thenReturn(List.of(rule));

        assertThat(service.resolve(TYPE_ID, Set.of(100L)).staticApproversExclusive()).isFalse();
    }

    @Test
    void exclusiveNeedsAtLeastOneOrg() {
        defaultRule(true);
        TenantWorkflowRoutingRule emptyRule = rule(1L, List.of(100L), List.of());
        when(ruleRepository.findByDefinitionIdOrderByIdAsc(DEFINITION_ID))
                .thenReturn(List.of(emptyRule));

        assertThat(service.resolve(TYPE_ID, Set.of()).staticApproversExclusive()).isFalse();
        Routing routing = service.resolve(TYPE_ID, Set.of(100L));
        assertThat(routing.staticOrganizationIds()).isEmpty();
        assertThat(routing.staticApproversExclusive()).isFalse();
    }

    @Test
    void missingTenantDefinitionYieldsNoExtraOrgs() {
        when(definitionRepository.findByWorkflowTypeId(TYPE_ID)).thenReturn(Optional.empty());

        assertThat(service.resolve(TYPE_ID, Set.of(1L))).isEqualTo(Routing.none());
    }

    private void defaultRule(boolean exclusive, Long... orgIds) {
        definition.setStaticApproversExclusive(exclusive);
        for (Long orgId : orgIds) {
            definition
                    .getStaticOrganizations()
                    .add(
                            TenantWorkflowDefinitionStaticOrg.builder()
                                    .tenantId(7L)
                                    .definition(definition)
                                    .approverOrganizationId(orgId)
                                    .build());
        }
    }

    private TenantWorkflowRoutingRule rule(long id, List<Long> subjectIds, List<Long> orgIds) {
        TenantWorkflowRoutingRule rule =
                TenantWorkflowRoutingRule.builder()
                        .id(id)
                        .tenantId(7L)
                        .definition(definition)
                        .build();
        for (Long subjectId : subjectIds) {
            rule.getSubjects()
                    .add(
                            TenantWorkflowRoutingRuleSubject.builder()
                                    .tenantId(7L)
                                    .definitionId(DEFINITION_ID)
                                    .rule(rule)
                                    .subjectId(subjectId)
                                    .build());
        }
        for (Long orgId : orgIds) {
            rule.getOrganizations()
                    .add(
                            TenantWorkflowRoutingRuleOrg.builder()
                                    .tenantId(7L)
                                    .rule(rule)
                                    .approverOrganizationId(orgId)
                                    .build());
        }
        return rule;
    }
}
