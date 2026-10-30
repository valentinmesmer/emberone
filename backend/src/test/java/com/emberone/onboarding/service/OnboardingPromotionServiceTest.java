/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.onboarding.config.EmptyStepConfig;
import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboarding.domain.OnboardingInvitationStepData;
import com.emberone.onboarding.domain.OnboardingStepType;
import com.emberone.onboarding.repository.OnboardingInvitationStepDataRepository;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OnboardingPromotionServiceTest {

    @Mock OrganizationRepository organizationRepository;
    @Mock EffectiveStandardResolver effectiveStandardResolver;
    @Mock StandardOnboardingStepService standardOnboardingStepService;
    @Mock OnboardingInvitationStepDataRepository stepDataRepository;
    @Mock OnboardingStepHandlerRegistry handlerRegistry;
    @Mock OnboardingInvitationDraftStorageService invitationDraftStorageService;
    @Mock OnboardingStepHandler stepHandler;

    @InjectMocks OnboardingPromotionService service;

    @Test
    void promoteAllStepsDeletesDraftAttachmentStorageAfterPromotion() {
        OnboardingInvitation invitation =
                OnboardingInvitation.builder()
                        .id(7L)
                        .tenantId(1L)
                        .userId(42L)
                        .targetOrganizationId(99L)
                        .build();
        Organization organization = Organization.builder().id(99L).tenantId(1L).build();
        when(organizationRepository.findById(99L)).thenReturn(Optional.of(organization));
        Standard standard = Standard.builder().id(3L).tenantId(1L).build();
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.of(standard));
        when(standardOnboardingStepService.listSteps(standard))
                .thenReturn(
                        List.of(
                                StandardOnboardingStep.builder()
                                        .stepType(OnboardingStepType.CERTIFICATIONS)
                                        .config(Map.of())
                                        .build()));
        when(stepDataRepository.findByInvitationId(7L))
                .thenReturn(
                        List.of(
                                OnboardingInvitationStepData.builder()
                                        .stepType(OnboardingStepType.CERTIFICATIONS)
                                        .payload(Map.of())
                                        .build()));
        when(handlerRegistry.require(OnboardingStepType.CERTIFICATIONS)).thenReturn(stepHandler);
        when(stepHandler.parseConfig(Map.of())).thenReturn(EmptyStepConfig.INSTANCE);
        when(stepHandler.isFulfilled(any(), any())).thenReturn(false);

        service.promoteAllSteps(invitation);

        verify(stepHandler).promote(any(), eq(EmptyStepConfig.INSTANCE), any());
        verify(invitationDraftStorageService).deleteDraftFilesForInvitation(7L);
    }
}
