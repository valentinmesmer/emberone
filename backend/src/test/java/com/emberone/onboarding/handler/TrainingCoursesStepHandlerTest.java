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

import com.emberone.onboarding.config.EmptyStepConfig;
import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboarding.service.OnboardingAttachmentPromotionService;
import com.emberone.onboarding.service.OnboardingMainOrganizationSkipEvaluator;
import com.emberone.onboarding.spi.OnboardingPromotionContext;
import com.emberone.onboarding.spi.OnboardingStepFulfillmentContext;
import com.emberone.trainingcourse.service.TrainingCourseRecordService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrainingCoursesStepHandlerTest {

    @Mock OnboardingMainOrganizationSkipEvaluator mainOrganizationSkipEvaluator;
    @Mock TrainingCourseRecordService trainingCourseRecordService;
    @Mock OnboardingAttachmentPromotionService attachmentPromotionService;

    @InjectMocks TrainingCoursesStepHandler handler;

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
                                Map.of("records", List.of(Map.of("name", "Atemschutz")))))
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

        verify(trainingCourseRecordService, never())
                .createSubmissionFromOnboarding(
                        anyLong(), anyLong(), anyLong(), any(), any(), any(), any(), any(), any(),
                        any(), anyLong(), anyLong());
    }

    @Test
    void promoteCreatesSubmissionFromDraft() {
        when(mainOrganizationSkipEvaluator.hasOtherActiveMainOrganization(42L, 99L))
                .thenReturn(false);
        when(attachmentPromotionService.copyDraftAttachment(anyLong(), anyLong(), anyLong(), any()))
                .thenReturn(66L);

        handler.promote(
                promotionContext(),
                EmptyStepConfig.INSTANCE,
                Map.of(
                        "records",
                        List.of(
                                Map.of(
                                        "name",
                                        "Atemschutz",
                                        "tenantTrainingCourseTypeIds",
                                        List.of(3L),
                                        "startDate",
                                        "2024-01-01",
                                        "endDate",
                                        "2024-01-05",
                                        "location",
                                        "München",
                                        "hours",
                                        40))));

        verify(attachmentPromotionService).copyDraftAttachment(eq(1L), eq(42L), eq(7L), any());
        verify(trainingCourseRecordService)
                .createSubmissionFromOnboarding(
                        eq(1L),
                        eq(42L),
                        eq(99L),
                        eq("Atemschutz"),
                        eq(List.of(3L)),
                        any(),
                        any(),
                        eq("München"),
                        any(),
                        any(),
                        eq(7L),
                        eq(66L));
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
