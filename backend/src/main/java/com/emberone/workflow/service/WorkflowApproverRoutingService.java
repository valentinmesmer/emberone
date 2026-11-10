/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.service;

import com.emberone.workflow.tenant.TenantWorkflowDefinition;
import com.emberone.workflow.tenant.TenantWorkflowDefinitionRepository;
import com.emberone.workflow.tenant.TenantWorkflowDefinitionStaticOrg;
import com.emberone.workflow.tenant.TenantWorkflowRoutingRule;
import com.emberone.workflow.tenant.TenantWorkflowRoutingRuleOrg;
import com.emberone.workflow.tenant.TenantWorkflowRoutingRuleRepository;
import com.emberone.workflow.tenant.TenantWorkflowRoutingRuleSubject;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the tenant-wide extra approver orgs of a workflow request from the default rule on the
 * tenant definition and the routing rules per subtype. This is the only source of extra approver
 * orgs; the org definitions carry no copy.
 */
@Service
@RequiredArgsConstructor
public class WorkflowApproverRoutingService {

    /** Extra approver orgs (without duplicates, in rule order) and the "only these decide" flag. */
    public record Routing(List<Long> staticOrganizationIds, boolean staticApproversExclusive) {

        public static Routing none() {
            return new Routing(List.of(), false);
        }
    }

    private final TenantWorkflowDefinitionRepository definitionRepository;
    private final TenantWorkflowRoutingRuleRepository ruleRepository;

    /**
     * Per subject id the rule of that subject applies, else the default rule; no subject ids means
     * the default rule. The orgs are the union of all matched rules. "Only these orgs decide" is
     * the workflow-wide switch on the definition and only holds when the union is not empty.
     */
    @Transactional(readOnly = true)
    public Routing resolve(Long workflowTypeId, Collection<Long> subjectIds) {
        TenantWorkflowDefinition definition =
                definitionRepository.findByWorkflowTypeId(workflowTypeId).orElse(null);
        if (definition == null) {
            return Routing.none();
        }
        Map<Long, TenantWorkflowRoutingRule> ruleBySubject = new HashMap<>();
        if (subjectIds != null && !subjectIds.isEmpty()) {
            for (TenantWorkflowRoutingRule rule :
                    ruleRepository.findByDefinitionIdOrderByIdAsc(definition.getId())) {
                for (TenantWorkflowRoutingRuleSubject subject : rule.getSubjects()) {
                    ruleBySubject.put(subject.getSubjectId(), rule);
                }
            }
        }

        Set<Long> orgIds = new LinkedHashSet<>();
        boolean usesDefault = subjectIds == null || subjectIds.isEmpty();
        Set<TenantWorkflowRoutingRule> matchedRules = new LinkedHashSet<>();
        if (!usesDefault) {
            for (Long subjectId : subjectIds) {
                TenantWorkflowRoutingRule rule = ruleBySubject.get(subjectId);
                if (rule == null) {
                    usesDefault = true;
                } else {
                    matchedRules.add(rule);
                }
            }
        }
        if (usesDefault) {
            definition.getStaticOrganizations().stream()
                    .map(TenantWorkflowDefinitionStaticOrg::getApproverOrganizationId)
                    .forEach(orgIds::add);
        }
        for (TenantWorkflowRoutingRule rule : matchedRules) {
            rule.getOrganizations().stream()
                    .map(TenantWorkflowRoutingRuleOrg::getApproverOrganizationId)
                    .forEach(orgIds::add);
        }
        return new Routing(
                List.copyOf(orgIds), definition.isStaticApproversExclusive() && !orgIds.isEmpty());
    }
}
