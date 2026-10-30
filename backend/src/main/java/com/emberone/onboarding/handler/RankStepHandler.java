/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.handler;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.onboarding.config.EmptyStepConfig;
import com.emberone.onboarding.config.OnboardingStepConfig;
import com.emberone.onboarding.domain.OnboardingStepType;
import com.emberone.onboarding.payload.OnboardingStepPayloads;
import com.emberone.onboarding.service.OnboardingAttachmentPromotionService;
import com.emberone.onboarding.service.OnboardingMainOrganizationSkipEvaluator;
import com.emberone.onboarding.spi.OnboardingPromotionContext;
import com.emberone.onboarding.spi.OnboardingStepFulfillmentContext;
import com.emberone.onboarding.spi.OnboardingStepHandler;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.i18n.LocalizationService;
import com.emberone.platform.i18n.TenantLocaleService;
import com.emberone.rank.service.UserRankService;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankStepHandler implements OnboardingStepHandler {

    private final OnboardingMainOrganizationSkipEvaluator mainOrganizationSkipEvaluator;
    private final UserRankService userRankService;
    private final UserOrganizationMembershipRepository membershipRepository;
    private final LocalizationService localizationService;
    private final TenantLocaleService tenantLocaleService;
    private final OnboardingAttachmentPromotionService attachmentPromotionService;

    @Override
    public OnboardingStepType type() {
        return OnboardingStepType.RANK;
    }

    @Override
    public OnboardingStepConfig parseConfig(Map<String, Object> raw) {
        return EmptyStepConfig.INSTANCE;
    }

    @Override
    public void validateConfig(long tenantId, OnboardingStepConfig config) {
        requireEmpty(config);
    }

    @Override
    public Map<String, Object> toConfigMap(OnboardingStepConfig config) {
        requireEmpty(config);
        return Map.of();
    }

    @Override
    public boolean isFulfilled(
            OnboardingStepFulfillmentContext context, OnboardingStepConfig config) {
        requireEmpty(config);
        if (mainOrganizationSkipEvaluator.hasOtherActiveMainOrganization(
                context.userId(), context.organizationId())) {
            return true;
        }
        return userRankService.hasOpenRank(context.userId());
    }

    @Override
    public void validateOnboardingPayload(
            OnboardingStepConfig config, Map<String, Object> payload) {
        requireEmpty(config);
        if (OnboardingStepPayloads.readBoolean(payload, OnboardingStepPayloads.NONE_DECLARED)) {
            return;
        }
        Long tenantRankId =
                OnboardingStepPayloads.readLong(payload, OnboardingStepPayloads.TENANT_RANK_ID);
        if (tenantRankId == null) {
            return;
        }
        if (tenantRankId <= 0) {
            throw new ConflictException("tenantRankId is invalid");
        }
    }

    @Override
    public Map<String, Object> normalizeOnboardingPayload(
            OnboardingStepConfig config, Map<String, Object> payload) {
        requireEmpty(config);
        Map<String, Object> normalized = OnboardingStepPayloads.emptyPayload();
        boolean noneDeclared =
                OnboardingStepPayloads.readBoolean(payload, OnboardingStepPayloads.NONE_DECLARED);
        Long tenantRankId =
                OnboardingStepPayloads.readLong(payload, OnboardingStepPayloads.TENANT_RANK_ID);
        normalized.put(OnboardingStepPayloads.TENANT_RANK_ID, noneDeclared ? null : tenantRankId);
        normalized.put(OnboardingStepPayloads.NONE_DECLARED, noneDeclared);
        Map<String, Object> rank =
                OnboardingStepPayloads.readObject(payload, OnboardingStepPayloads.RANK);
        if (rank != null) {
            Map<String, Object> rankMap = new LinkedHashMap<>();
            rankMap.put(
                    OnboardingStepPayloads.ATTACHMENTS,
                    OnboardingStepPayloads.readObjectList(
                            rank, OnboardingStepPayloads.ATTACHMENTS));
            normalized.put(OnboardingStepPayloads.RANK, rankMap);
        }
        return normalized;
    }

    @Override
    public boolean isOnboardingFulfilled(
            OnboardingStepFulfillmentContext context,
            OnboardingStepConfig config,
            Map<String, Object> payload) {
        requireEmpty(config);
        if (OnboardingStepPayloads.readLong(payload, OnboardingStepPayloads.TENANT_RANK_ID)
                != null) {
            return true;
        }
        return OnboardingStepPayloads.readBoolean(payload, OnboardingStepPayloads.NONE_DECLARED);
    }

    @Override
    public void promote(
            OnboardingPromotionContext context,
            OnboardingStepConfig config,
            Map<String, Object> payload) {
        requireEmpty(config);
        OnboardingStepFulfillmentContext fulfillmentContext =
                new OnboardingStepFulfillmentContext(
                        context.tenantId(),
                        context.userId(),
                        context.organizationId(),
                        context.invitationId());
        if (isFulfilled(fulfillmentContext, config)
                || OnboardingStepPayloads.readBoolean(
                        payload, OnboardingStepPayloads.NONE_DECLARED)) {
            return;
        }
        Long tenantRankId =
                OnboardingStepPayloads.readLong(payload, OnboardingStepPayloads.TENANT_RANK_ID);
        if (tenantRankId == null) {
            return;
        }
        Map<String, Object> rank =
                OnboardingStepPayloads.readObject(payload, OnboardingStepPayloads.RANK);
        if (rank != null) {
            List<Map<String, Object>> attachments = OnboardingStepPayloads.readAttachments(rank);
            if (!attachments.isEmpty()) {
                attachmentPromotionService.copyDraftAttachment(
                        context.tenantId(),
                        context.userId(),
                        context.invitationId(),
                        attachments.getFirst());
            }
        }
        LocalDate startDate =
                resolveMainMembershipStartDate(context.userId(), context.organizationId());
        SupportedLocale locale = tenantLocaleService.resolveTenantLocale(context.tenantId());
        String reason = localizationService.getMessage("rank.onboarding.reason", locale);
        userRankService.assignFromOnboarding(context.userId(), tenantRankId, startDate, reason);
    }

    private LocalDate resolveMainMembershipStartDate(long userId, long organizationId) {
        Instant now = Instant.now();
        UserOrganizationMembership membership =
                membershipRepository
                        .findActiveByUser_IdAndMainTrue(userId, now)
                        .or(
                                () ->
                                        membershipRepository.findActiveByUser_IdAndOrganization_Id(
                                                userId, organizationId, now))
                        .orElseThrow(
                                () ->
                                        new ConflictException(
                                                "No active membership for rank promotion"));
        return membership.getStartDate().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static void requireEmpty(OnboardingStepConfig config) {
        if (!(config instanceof EmptyStepConfig)) {
            throw new IllegalArgumentException("Expected empty step config");
        }
    }
}
