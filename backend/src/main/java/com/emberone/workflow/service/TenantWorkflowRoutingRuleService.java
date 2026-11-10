/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.service;

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
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import com.emberone.workflow.tenant.TenantWorkflowDefinition;
import com.emberone.workflow.tenant.TenantWorkflowRoutingRule;
import com.emberone.workflow.tenant.TenantWorkflowRoutingRuleOrg;
import com.emberone.workflow.tenant.TenantWorkflowRoutingRuleRepository;
import com.emberone.workflow.tenant.TenantWorkflowRoutingRuleSubject;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Routing rules per subtype of a tenant workflow definition. Callers hold the tenant scope (see
 * {@link TenantWorkflowDefinitionService}); the rows are isolated by RLS.
 */
@Service
@RequiredArgsConstructor
public class TenantWorkflowRoutingRuleService {

    private final TenantWorkflowRoutingRuleRepository ruleRepository;
    private final WorkflowHandlerRegistry handlerRegistry;
    private final OrganizationRepository organizationRepository;
    private final TenantCertDefinitionRepository certDefinitionRepository;
    private final TenantTrainingCourseTypeRepository trainingCourseTypeRepository;
    private final TenantDrivingLicenseDefinitionRepository drivingLicenseDefinitionRepository;
    private final TenantRankRepository rankRepository;

    @Transactional(readOnly = true)
    public List<WorkflowRoutingRuleDto> list(TenantWorkflowDefinition definition) {
        return ruleRepository.findByDefinitionIdOrderByIdAsc(definition.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkflowRoutingRuleDto get(TenantWorkflowDefinition definition, Long ruleId) {
        return toDto(requireRule(definition, ruleId));
    }

    @Transactional
    public WorkflowRoutingRuleDto create(
            TenantWorkflowDefinition definition, SaveWorkflowRoutingRuleRequest request) {
        TenantWorkflowRoutingRule rule =
                TenantWorkflowRoutingRule.builder()
                        .tenantId(definition.getTenantId())
                        .definition(definition)
                        .build();
        apply(definition, rule, request);
        ruleRepository.save(rule);
        return toDto(rule);
    }

    @Transactional
    public WorkflowRoutingRuleDto update(
            TenantWorkflowDefinition definition,
            Long ruleId,
            SaveWorkflowRoutingRuleRequest request) {
        TenantWorkflowRoutingRule rule = requireRule(definition, ruleId);
        apply(definition, rule, request);
        ruleRepository.saveAndFlush(rule);
        return toDto(rule);
    }

    @Transactional
    public void delete(TenantWorkflowDefinition definition, Long ruleId) {
        ruleRepository.delete(requireRule(definition, ruleId));
    }

    @Transactional
    public void deleteAll(TenantWorkflowDefinition definition) {
        ruleRepository.deleteAll(ruleRepository.findByDefinitionIdOrderByIdAsc(definition.getId()));
    }

    private TenantWorkflowRoutingRule requireRule(
            TenantWorkflowDefinition definition, Long ruleId) {
        return ruleRepository
                .findByIdAndDefinitionId(ruleId, definition.getId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Routing rule not found: " + ruleId));
    }

    private void apply(
            TenantWorkflowDefinition definition,
            TenantWorkflowRoutingRule rule,
            SaveWorkflowRoutingRuleRequest request) {
        Long tenantId = definition.getTenantId();
        WorkflowRoutingDimension dimension =
                handlerRegistry
                        .require(definition.getWorkflowTypeId())
                        .routingDimension()
                        .orElseThrow(() ->
                                new ValidationException(
                                        "This workflow type cannot be split into rules"));

        Set<Long> subjectIds = new LinkedHashSet<>(request.getSubjectIds());
        if (subjectIds.isEmpty() || subjectIds.contains(null)) {
            throw new ValidationException("At least one valid subject is required");
        }
        for (Long subjectId : subjectIds) {
            if (!existsInCatalog(dimension, subjectId)) {
                throw new ValidationException(
                        "Subject " + subjectId + " does not exist in the tenant catalog");
            }
        }
        for (TenantWorkflowRoutingRule other :
                ruleRepository.findByDefinitionIdOrderByIdAsc(definition.getId())) {
            if (other.getId() != null && other.getId().equals(rule.getId())) {
                continue;
            }
            for (TenantWorkflowRoutingRuleSubject taken : other.getSubjects()) {
                if (subjectIds.contains(taken.getSubjectId())) {
                    throw new ValidationException(
                            "Subject " + taken.getSubjectId() + " is already used by another rule");
                }
            }
        }

        Set<Long> orgIds =
                request.getStaticApproverOrganizationIds() == null
                        ? Set.of()
                        : new LinkedHashSet<>(request.getStaticApproverOrganizationIds());
        for (Long orgId : orgIds) {
            if (orgId == null || !organizationRepository.existsByIdAndTenantId(orgId, tenantId)) {
                throw new ValidationException(
                        "Organization " + orgId + " does not belong to the tenant");
            }
        }

        rule.getSubjects().removeIf(existing -> !subjectIds.contains(existing.getSubjectId()));
        for (Long subjectId : subjectIds) {
            if (rule.getSubjects().stream().noneMatch(s -> s.getSubjectId().equals(subjectId))) {
                rule.getSubjects()
                        .add(
                                TenantWorkflowRoutingRuleSubject.builder()
                                        .tenantId(tenantId)
                                        .definitionId(definition.getId())
                                        .rule(rule)
                                        .subjectId(subjectId)
                                        .build());
            }
        }
        rule.getOrganizations()
                .removeIf(existing -> !orgIds.contains(existing.getApproverOrganizationId()));
        for (Long orgId : orgIds) {
            if (rule.getOrganizations().stream()
                    .noneMatch(o -> o.getApproverOrganizationId().equals(orgId))) {
                rule.getOrganizations()
                        .add(
                                TenantWorkflowRoutingRuleOrg.builder()
                                        .tenantId(tenantId)
                                        .rule(rule)
                                        .approverOrganizationId(orgId)
                                        .build());
            }
        }
    }

    private boolean existsInCatalog(WorkflowRoutingDimension dimension, Long subjectId) {
        return switch (dimension) {
            case CERTIFICATION_DEFINITION -> certDefinitionRepository.existsById(subjectId);
            case TRAINING_COURSE_TYPE -> trainingCourseTypeRepository.existsById(subjectId);
            case DRIVING_LICENSE_DEFINITION ->
                    drivingLicenseDefinitionRepository.existsById(subjectId);
            case RANK -> rankRepository.existsById(subjectId);
        };
    }

    private WorkflowRoutingRuleDto toDto(TenantWorkflowRoutingRule rule) {
        return WorkflowRoutingRuleDto.builder()
                .id(rule.getId())
                .subjectIds(
                        rule.getSubjects().stream()
                                .map(TenantWorkflowRoutingRuleSubject::getSubjectId)
                                .sorted(Comparator.naturalOrder())
                                .toList())
                .staticApproverOrganizationIds(
                        rule.getOrganizations().stream()
                                .map(TenantWorkflowRoutingRuleOrg::getApproverOrganizationId)
                                .sorted(Comparator.naturalOrder())
                                .toList())
                .build();
    }
}
