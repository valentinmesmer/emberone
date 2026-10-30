/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.service;

import com.emberone.onboarding.config.OnboardingStepConfig;
import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboarding.domain.OnboardingInvitationStatus;
import com.emberone.onboarding.domain.OnboardingInvitationStepData;
import com.emberone.onboarding.domain.OnboardingStepType;
import com.emberone.onboarding.repository.OnboardingInvitationRepository;
import com.emberone.onboarding.repository.OnboardingInvitationStepDataRepository;
import com.emberone.onboarding.spi.OnboardingStepFulfillmentContext;
import com.emberone.onboarding.spi.OnboardingStepHandler;
import com.emberone.onboarding.spi.OnboardingStepHandlerRegistry;
import com.emberone.onboarding.standard.StandardOnboardingStep;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.standard.service.EffectiveStandardResolver;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingInvitationReadinessService {

    private final OnboardingInvitationRepository invitationRepository;
    private final OnboardingInvitationStepDataRepository stepDataRepository;
    private final OrganizationRepository organizationRepository;
    private final EffectiveStandardResolver effectiveStandardResolver;
    private final StandardOnboardingStepService standardOnboardingStepService;
    private final OnboardingStepHandlerRegistry handlerRegistry;

    @Transactional(readOnly = true)
    public boolean isReadyToSubmit(long userId, long invitationId) {
        OnboardingInvitation invitation = requireOwnInvitation(userId, invitationId);
        if (invitation.getStatus() != OnboardingInvitationStatus.OPEN) {
            return false;
        }
        return areAllStepsFulfilled(invitation);
    }

    /**
     * Whether the user already satisfies every configured onboarding step via live user tables (no
     * invitation drafts). Empty step configuration counts as satisfied - nothing remains to
     * collect.
     */
    @Transactional(readOnly = true)
    public boolean hasConfiguredOnboardingSteps(Organization organization) {
        return effectiveStandardResolver
                .resolveForOrganization(organization)
                .map(standardOnboardingStepService::listSteps)
                .map(steps -> !steps.isEmpty())
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean hasConfiguredOnboardingSteps(long organizationId) {
        Organization organization =
                organizationRepository
                        .findById(organizationId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Organization not found: " + organizationId));
        return hasConfiguredOnboardingSteps(organization);
    }

    @Transactional(readOnly = true)
    public boolean areAllStepsLiveFulfilled(long tenantId, long userId, long targetOrganizationId) {
        Organization organization =
                organizationRepository
                        .findById(targetOrganizationId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Organization not found: " + targetOrganizationId));
        if (!organization.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Organization not found: " + targetOrganizationId);
        }
        List<StandardOnboardingStep> configuredSteps =
                effectiveStandardResolver
                        .resolveForOrganization(organization)
                        .map(standardOnboardingStepService::listSteps)
                        .orElse(List.of());
        if (configuredSteps.isEmpty()) {
            return true;
        }
        OnboardingStepFulfillmentContext context =
                new OnboardingStepFulfillmentContext(tenantId, userId, targetOrganizationId, 0L);
        for (StandardOnboardingStep step : configuredSteps) {
            OnboardingStepHandler handler = handlerRegistry.require(step.getStepType());
            OnboardingStepConfig config = handler.parseConfig(step.getConfig());
            if (!handler.isFulfilled(context, config)) {
                return false;
            }
        }
        return true;
    }

    @Transactional(readOnly = true)
    public boolean areAllStepsFulfilled(OnboardingInvitation invitation) {
        Organization organization =
                organizationRepository
                        .findById(invitation.getTargetOrganizationId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
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
        OnboardingStepFulfillmentContext context =
                new OnboardingStepFulfillmentContext(
                        invitation.getTenantId(),
                        invitation.getUserId(),
                        invitation.getTargetOrganizationId(),
                        invitation.getId());
        return areAllStepsFulfilled(configuredSteps, dataByType, context);
    }

    @Transactional(readOnly = true)
    public boolean areAllStepsFulfilled(
            List<StandardOnboardingStep> configuredSteps,
            Map<OnboardingStepType, OnboardingInvitationStepData> dataByType,
            OnboardingStepFulfillmentContext context) {
        if (configuredSteps.isEmpty()) {
            return true;
        }
        for (StandardOnboardingStep step : configuredSteps) {
            OnboardingStepHandler handler = handlerRegistry.require(step.getStepType());
            OnboardingStepConfig config = handler.parseConfig(step.getConfig());
            if (handler.isFulfilled(context, config)) {
                continue;
            }
            OnboardingInvitationStepData data = dataByType.get(step.getStepType());
            Map<String, Object> payload = data == null ? Map.of() : data.getPayload();
            if (!handler.isOnboardingFulfilled(context, config, payload)) {
                return false;
            }
        }
        return true;
    }

    private OnboardingInvitation requireOwnInvitation(long userId, long invitationId) {
        return invitationRepository
                .findByIdAndUserId(invitationId, userId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Onboarding invitation not found: " + invitationId));
    }
}
