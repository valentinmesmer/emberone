/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.drivinglicense.service.DrivingLicenseRecordService;
import com.emberone.onboarding.config.EmptyStepConfig;
import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboarding.service.OnboardingAttachmentPromotionService;
import com.emberone.onboarding.service.OnboardingMainOrganizationSkipEvaluator;
import com.emberone.onboarding.spi.OnboardingPromotionContext;
import com.emberone.onboarding.spi.OnboardingStepFulfillmentContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DrivingLicenseStepHandlerTest {

    @Mock OnboardingMainOrganizationSkipEvaluator mainOrganizationSkipEvaluator;
    @Mock DrivingLicenseRecordService drivingLicenseRecordService;
    @Mock OnboardingAttachmentPromotionService attachmentPromotionService;

    @InjectMocks DrivingLicenseStepHandler handler;

    @Test
    void isFulfilledWhenUserHasOtherActiveMainOrganization() {
        when(mainOrganizationSkipEvaluator.hasOtherActiveMainOrganization(42L, 99L))
                .thenReturn(true);

        assertThat(
                        handler.isFulfilled(
                                new OnboardingStepFulfillmentContext(1L, 42L, 99L, 7L),
                                EmptyStepConfig.INSTANCE))
                .isTrue();
    }

    @Test
    void isOnboardingFulfilledWhenLicensePresent() {
        assertThat(
                        handler.isOnboardingFulfilled(
                                new OnboardingStepFulfillmentContext(1L, 42L, 99L, 7L),
                                EmptyStepConfig.INSTANCE,
                                Map.of("license", Map.of("licenseNumber", "B123"))))
                .isTrue();
    }

    @Test
    void isOnboardingFulfilledWhenNoneDeclared() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("license", null);
        payload.put("noneDeclared", true);

        assertThat(
                        handler.isOnboardingFulfilled(
                                new OnboardingStepFulfillmentContext(1L, 42L, 99L, 7L),
                                EmptyStepConfig.INSTANCE,
                                payload))
                .isTrue();
    }

    @Test
    void isOnboardingNotFulfilledWhenEmptyWithoutDeclaration() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("license", null);

        assertThat(
                        handler.isOnboardingFulfilled(
                                new OnboardingStepFulfillmentContext(1L, 42L, 99L, 7L),
                                EmptyStepConfig.INSTANCE,
                                payload))
                .isFalse();
    }

    @Test
    void promoteSkipsWhenNoneDeclared() {
        handler.promote(promotionContext(), EmptyStepConfig.INSTANCE, Map.of("noneDeclared", true));

        verify(drivingLicenseRecordService, never())
                .upsertFromOnboarding(
                        anyLong(), anyLong(), anyLong(), any(), any(), any(), any(), any(), any(),
                        anyLong(), anyLong());
    }

    @Test
    void promoteUpsertsLicenseFromDraft() {
        when(mainOrganizationSkipEvaluator.hasOtherActiveMainOrganization(42L, 99L))
                .thenReturn(false);
        when(attachmentPromotionService.copyDraftAttachment(anyLong(), anyLong(), anyLong(), any()))
                .thenReturn(55L);

        handler.promote(
                promotionContext(),
                EmptyStepConfig.INSTANCE,
                Map.of(
                        "license",
                        Map.of(
                                "issueDate",
                                "2020-01-01",
                                "licenseNumber",
                                "B123",
                                "issuingAuthority",
                                "Stadt",
                                "classes",
                                List.of())));

        verify(attachmentPromotionService).copyDraftAttachment(eq(1L), eq(42L), eq(7L), any());
        verify(drivingLicenseRecordService)
                .upsertFromOnboarding(
                        eq(1L),
                        eq(42L),
                        eq(99L),
                        any(),
                        any(),
                        eq("B123"),
                        eq("Stadt"),
                        any(),
                        any(),
                        eq(7L),
                        eq(55L));
    }

    private static OnboardingPromotionContext promotionContext() {
        OnboardingInvitation invitation =
                OnboardingInvitation.builder()
                        .id(7L)
                        .tenantId(1L)
                        .userId(42L)
                        .targetOrganizationId(99L)
                        .build();
        return new OnboardingPromotionContext(invitation, 1L, 42L, 99L);
    }
}
