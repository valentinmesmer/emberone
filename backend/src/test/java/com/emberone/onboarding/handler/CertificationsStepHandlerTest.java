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

import com.emberone.certification.service.CertificationAssignmentService;
import com.emberone.certification.service.CertificationRecordService;
import com.emberone.onboarding.config.EmptyStepConfig;
import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboarding.service.OnboardingAttachmentPromotionService;
import com.emberone.onboarding.service.OnboardingMainOrganizationSkipEvaluator;
import com.emberone.onboarding.spi.OnboardingPromotionContext;
import com.emberone.onboarding.spi.OnboardingStepFulfillmentContext;
import com.emberone.platform.security.TenantContextHolder;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CertificationsStepHandlerTest {

    @Mock OnboardingMainOrganizationSkipEvaluator mainOrganizationSkipEvaluator;
    @Mock CertificationAssignmentService certificationAssignmentService;
    @Mock CertificationRecordService certificationRecordService;
    @Mock OnboardingAttachmentPromotionService attachmentPromotionService;

    @InjectMocks CertificationsStepHandler handler;

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
    void isOnboardingFulfilledWhenRecordPresent() {
        assertThat(
                        handler.isOnboardingFulfilled(
                                new OnboardingStepFulfillmentContext(1L, 42L, 99L, 7L),
                                EmptyStepConfig.INSTANCE,
                                Map.of("records", List.of(Map.of("tenantCertDefinitionId", 1)))))
                .isTrue();
    }

    @Test
    void isOnboardingFulfilledWhenNoneDeclared() {
        assertThat(
                        handler.isOnboardingFulfilled(
                                new OnboardingStepFulfillmentContext(1L, 42L, 99L, 7L),
                                EmptyStepConfig.INSTANCE,
                                Map.of("records", List.of(), "noneDeclared", true)))
                .isTrue();
    }

    @Test
    void isOnboardingNotFulfilledWhenEmptyWithoutDeclaration() {
        assertThat(
                        handler.isOnboardingFulfilled(
                                new OnboardingStepFulfillmentContext(1L, 42L, 99L, 7L),
                                EmptyStepConfig.INSTANCE,
                                Map.of("records", List.of())))
                .isFalse();
    }

    @Test
    void promoteSkipsWhenNoneDeclared() {
        handler.promote(promotionContext(), EmptyStepConfig.INSTANCE, Map.of("noneDeclared", true));

        verify(certificationRecordService, never())
                .createFromOnboarding(
                        anyLong(), anyLong(), anyLong(), any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    void promoteCreatesAssignmentsAndRecordsFromDraft() {
        when(mainOrganizationSkipEvaluator.hasOtherActiveMainOrganization(42L, 99L))
                .thenReturn(false);
        when(attachmentPromotionService.copyDraftAttachment(anyLong(), anyLong(), anyLong(), any()))
                .thenReturn(88L);

        handler.promote(
                promotionContext(),
                EmptyStepConfig.INSTANCE,
                Map.of(
                        "records",
                        List.of(
                                Map.of(
                                        "tenantCertDefinitionId",
                                        12L,
                                        "completedDate",
                                        "2024-06-01"))));

        verify(attachmentPromotionService).copyDraftAttachment(eq(1L), eq(42L), eq(7L), any());
        verify(certificationAssignmentService)
                .createAssignmentsFromOnboarding(1L, 42L, 99L, 7L, List.of(12L));
        verify(certificationRecordService)
                .createFromOnboarding(
                        eq(1L), eq(42L), eq(12L), any(), any(), any(), eq(7L), eq(88L));
    }

    @Test
    void promoteCopiesScanForHealthEvidence() {
        when(mainOrganizationSkipEvaluator.hasOtherActiveMainOrganization(42L, 99L))
                .thenReturn(false);
        when(attachmentPromotionService.copyDraftAttachment(eq(1L), eq(42L), eq(7L), any()))
                .thenReturn(88L);

        handler.promote(
                promotionContext(),
                EmptyStepConfig.INSTANCE,
                Map.of(
                        "records",
                        List.of(
                                Map.of(
                                        "tenantCertDefinitionId",
                                        12L,
                                        "completedDate",
                                        "2024-06-01"))));

        verify(attachmentPromotionService).copyDraftAttachment(eq(1L), eq(42L), eq(7L), any());
        verify(certificationRecordService)
                .createFromOnboarding(
                        eq(1L), eq(42L), eq(12L), any(), any(), any(), eq(7L), eq(88L));
    }

    @Test
    void validateOnboardingPayloadChecksExpiryRuleForCapture() {
        TenantContextHolder.setTenantId(1L);
        try {
            handler.validateOnboardingPayload(
                    EmptyStepConfig.INSTANCE,
                    Map.of(
                            "records",
                            List.of(
                                    Map.of(
                                            "tenantCertDefinitionId",
                                            12L,
                                            "completedDate",
                                            "2024-06-01"))));
        } finally {
            TenantContextHolder.clear();
        }

        verify(certificationRecordService)
                .validateExpiryForCapture(12L, LocalDate.of(2024, 6, 1), null);
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
