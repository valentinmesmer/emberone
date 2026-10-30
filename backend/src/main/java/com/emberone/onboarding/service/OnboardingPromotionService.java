/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.service;

import com.emberone.onboarding.config.OnboardingStepConfig;
import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboarding.domain.OnboardingInvitationStepData;
import com.emberone.onboarding.domain.OnboardingStepType;
import com.emberone.onboarding.repository.OnboardingInvitationStepDataRepository;
import com.emberone.onboarding.spi.OnboardingPromotionContext;
import com.emberone.onboarding.spi.OnboardingStepFulfillmentContext;
import com.emberone.onboarding.spi.OnboardingStepHandler;
import com.emberone.onboarding.spi.OnboardingStepHandlerRegistry;
import com.emberone.onboarding.standard.StandardOnboardingStep;
import com.emberone.onboardingdocument.domain.EntryDocumentFulfillmentChannel;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.standard.service.EffectiveStandardResolver;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingPromotionService {

    private final OrganizationRepository organizationRepository;
    private final EffectiveStandardResolver effectiveStandardResolver;
    private final StandardOnboardingStepService standardOnboardingStepService;
    private final OnboardingInvitationStepDataRepository stepDataRepository;
    private final OnboardingStepHandlerRegistry handlerRegistry;
    private final OnboardingInvitationDraftStorageService invitationDraftStorageService;

    @Transactional
    public void promoteAllSteps(OnboardingInvitation invitation) {
        promoteAllSteps(invitation, null, EntryDocumentFulfillmentChannel.SELF, null);
    }

    @Transactional
    public void promoteAllSteps(OnboardingInvitation invitation, Long approvedByUserId) {
        promoteAllSteps(invitation, approvedByUserId, EntryDocumentFulfillmentChannel.SELF, null);
    }

    @Transactional
    public void promoteAllSteps(
            OnboardingInvitation invitation,
            Long workflowReviewerUserId,
            EntryDocumentFulfillmentChannel entryDocumentChannel,
            String adminRecordReason) {
        Organization organization =
                organizationRepository
                        .findById(invitation.getTargetOrganizationId())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Organization not found: "
                                                        + invitation.getTargetOrganizationId()));
        List<StandardOnboardingStep> configuredSteps =
                effectiveStandardResolver
                        .resolveForOrganization(organization)
                        .map(standardOnboardingStepService::listSteps)
                        .orElse(List.of());
        Map<OnboardingStepType, OnboardingInvitationStepData> dataByType =
                stepDataRepository.findByInvitationId(invitation.getId()).stream()
                        .collect(
                                Collectors.toMap(
                                        OnboardingInvitationStepData::getStepType,
                                        Function.identity()));
        OnboardingPromotionContext promotionContext =
                new OnboardingPromotionContext(
                        invitation,
                        invitation.getTenantId(),
                        invitation.getUserId(),
                        invitation.getTargetOrganizationId(),
                        workflowReviewerUserId,
                        entryDocumentChannel,
                        adminRecordReason);
        OnboardingStepFulfillmentContext fulfillmentContext =
                new OnboardingStepFulfillmentContext(
                        invitation.getTenantId(),
                        invitation.getUserId(),
                        invitation.getTargetOrganizationId(),
                        invitation.getId());

        for (StandardOnboardingStep step : configuredSteps) {
            OnboardingStepHandler handler = handlerRegistry.require(step.getStepType());
            OnboardingStepConfig config = handler.parseConfig(step.getConfig());
            OnboardingInvitationStepData data = dataByType.get(step.getStepType());
            Map<String, Object> payload = data == null ? Map.of() : data.getPayload();
            if (handler.isFulfilled(fulfillmentContext, config)
                    && !handler.hasInvitationWork(config, payload)) {
                continue;
            }
            handler.promote(promotionContext, config, payload);
        }
        invitationDraftStorageService.deleteDraftFilesForInvitation(invitation.getId());
        log.debug("Promoted onboarding steps for invitation {}", invitation.getId());
    }
}
