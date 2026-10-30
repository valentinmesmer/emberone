/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.federalstate.baseline;

import com.emberone.onboarding.domain.OnboardingStepType;
import java.util.LinkedHashSet;
import java.util.List;

public record TenantBaseline(
        List<CertificationDefinitionBaseline> certificationDefinitions,
        List<TrainingCourseTypeBaseline> trainingCourseTypes,
        List<DrivingLicenseDefinitionBaseline> drivingLicenseDefinitions,
        List<QualificationDefinitionBaseline> qualificationDefinitions,
        List<WorkflowDefinitionBaseline> workflowDefinitions,
        List<OrganizationTypeDefinitionBaseline> organizationTypeDefinitions,
        List<OnboardingDocumentDefinitionBaseline> onboardingDocumentDefinitions,
        List<RankBaseline> ranks,
        List<FunctionBaseline> functions,
        List<FunctionGrantBaseline> functionGrants,
        List<OnboardingStepType> systemOnboardingStepTypes) {

    public TenantBaseline {
        certificationDefinitions =
                certificationDefinitions == null
                        ? List.of()
                        : List.copyOf(certificationDefinitions);
        trainingCourseTypes =
                trainingCourseTypes == null ? List.of() : List.copyOf(trainingCourseTypes);
        drivingLicenseDefinitions =
                drivingLicenseDefinitions == null
                        ? List.of()
                        : List.copyOf(drivingLicenseDefinitions);
        qualificationDefinitions =
                qualificationDefinitions == null
                        ? List.of()
                        : List.copyOf(qualificationDefinitions);
        workflowDefinitions =
                workflowDefinitions == null ? List.of() : List.copyOf(workflowDefinitions);
        organizationTypeDefinitions =
                organizationTypeDefinitions == null
                        ? List.of()
                        : List.copyOf(organizationTypeDefinitions);
        onboardingDocumentDefinitions =
                onboardingDocumentDefinitions == null
                        ? List.of()
                        : List.copyOf(onboardingDocumentDefinitions);
        ranks = ranks == null ? List.of() : List.copyOf(ranks);
        functions = functions == null ? List.of() : List.copyOf(functions);
        functionGrants = functionGrants == null ? List.of() : List.copyOf(functionGrants);
        systemOnboardingStepTypes =
                systemOnboardingStepTypes == null
                        ? List.of()
                        : List.copyOf(systemOnboardingStepTypes);
    }

    public List<String> certificationConfigKeys() {
        return certificationDefinitions.stream()
                .map(CertificationDefinitionBaseline::configKey)
                .toList();
    }

    public List<String> trainingCourseTypeConfigKeys() {
        return trainingCourseTypes.stream().map(TrainingCourseTypeBaseline::configKey).toList();
    }

    public List<String> drivingLicenseDefinitionConfigKeys() {
        return drivingLicenseDefinitions.stream()
                .map(DrivingLicenseDefinitionBaseline::configKey)
                .toList();
    }

    public List<String> qualificationConfigKeys() {
        return qualificationDefinitions.stream()
                .map(QualificationDefinitionBaseline::configKey)
                .toList();
    }

    public List<String> onboardingDocumentConfigKeys() {
        return onboardingDocumentDefinitions.stream()
                .map(OnboardingDocumentDefinitionBaseline::configKey)
                .toList();
    }

    public List<String> rankConfigKeys() {
        return ranks.stream().map(RankBaseline::configKey).toList();
    }

    public List<String> functionConfigKeys() {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (FunctionBaseline item : functions) {
            keys.add(item.configKey());
        }
        for (FunctionGrantBaseline grant : functionGrants) {
            keys.add(grant.functionConfigKey());
        }
        return List.copyOf(keys);
    }

    public record OrganizationTypeDefinitionBaseline(String configKey, String name) {}
}
