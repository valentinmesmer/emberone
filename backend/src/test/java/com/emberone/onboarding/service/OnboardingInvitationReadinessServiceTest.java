/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.onboarding.config.EmptyStepConfig;
import com.emberone.onboarding.domain.OnboardingStepType;
import com.emberone.onboarding.repository.OnboardingInvitationRepository;
import com.emberone.onboarding.repository.OnboardingInvitationStepDataRepository;
import com.emberone.onboarding.spi.OnboardingStepFulfillmentContext;
import com.emberone.onboarding.spi.OnboardingStepHandler;
import com.emberone.onboarding.spi.OnboardingStepHandlerRegistry;
import com.emberone.onboarding.standard.StandardOnboardingStep;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.service.EffectiveStandardResolver;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OnboardingInvitationReadinessServiceTest {

    @Mock OnboardingInvitationRepository invitationRepository;
    @Mock OnboardingInvitationStepDataRepository stepDataRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock EffectiveStandardResolver effectiveStandardResolver;
    @Mock StandardOnboardingStepService standardOnboardingStepService;
    @Mock OnboardingStepHandlerRegistry handlerRegistry;
    @Mock OnboardingStepHandler trainingCoursesHandler;

    private OnboardingInvitationReadinessService service;

    @BeforeEach
    void setUp() {
        service =
                new OnboardingInvitationReadinessService(
                        invitationRepository,
                        stepDataRepository,
                        organizationRepository,
                        effectiveStandardResolver,
                        standardOnboardingStepService,
                        handlerRegistry);
    }

    @Test
    void areAllStepsLiveFulfilledWhenNoStepsConfigured() {
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        Standard standard = Standard.builder().id(3L).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.of(standard));
        when(standardOnboardingStepService.listSteps(standard)).thenReturn(List.of());

        assertThat(service.areAllStepsLiveFulfilled(7L, 20L, 10L)).isTrue();
    }

    @Test
    void areAllStepsLiveFulfilledWhenEveryStepIsLiveFulfilled() {
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        Standard standard = Standard.builder().id(3L).build();
        StandardOnboardingStep step =
                StandardOnboardingStep.builder()
                        .stepType(OnboardingStepType.TRAINING_COURSES)
                        .config(Map.of())
                        .build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.of(standard));
        when(standardOnboardingStepService.listSteps(standard)).thenReturn(List.of(step));
        when(handlerRegistry.require(OnboardingStepType.TRAINING_COURSES))
                .thenReturn(trainingCoursesHandler);
        when(trainingCoursesHandler.parseConfig(any())).thenReturn(EmptyStepConfig.INSTANCE);
        when(trainingCoursesHandler.isFulfilled(
                        eq(new OnboardingStepFulfillmentContext(7L, 20L, 10L, 0L)),
                        eq(EmptyStepConfig.INSTANCE)))
                .thenReturn(true);

        assertThat(service.areAllStepsLiveFulfilled(7L, 20L, 10L)).isTrue();
        verify(trainingCoursesHandler)
                .isFulfilled(
                        eq(new OnboardingStepFulfillmentContext(7L, 20L, 10L, 0L)),
                        eq(EmptyStepConfig.INSTANCE));
    }

    @Test
    void areAllStepsLiveFulfilledFalseWhenAStepIsMissing() {
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        Standard standard = Standard.builder().id(3L).build();
        StandardOnboardingStep step =
                StandardOnboardingStep.builder()
                        .stepType(OnboardingStepType.TRAINING_COURSES)
                        .config(Map.of())
                        .build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.of(standard));
        when(standardOnboardingStepService.listSteps(standard)).thenReturn(List.of(step));
        when(handlerRegistry.require(OnboardingStepType.TRAINING_COURSES))
                .thenReturn(trainingCoursesHandler);
        when(trainingCoursesHandler.parseConfig(any())).thenReturn(EmptyStepConfig.INSTANCE);
        when(trainingCoursesHandler.isFulfilled(any(), eq(EmptyStepConfig.INSTANCE)))
                .thenReturn(false);

        assertThat(service.areAllStepsLiveFulfilled(7L, 20L, 10L)).isFalse();
    }

    @Test
    void hasConfiguredOnboardingStepsFalseWhenStepListEmpty() {
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        Standard standard = Standard.builder().id(3L).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.of(standard));
        when(standardOnboardingStepService.listSteps(standard)).thenReturn(List.of());

        assertThat(service.hasConfiguredOnboardingSteps(10L)).isFalse();
    }
}
