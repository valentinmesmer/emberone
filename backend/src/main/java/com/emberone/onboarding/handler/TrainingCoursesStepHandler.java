/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.handler;

import com.emberone.onboarding.config.EmptyStepConfig;
import com.emberone.onboarding.config.OnboardingStepConfig;
import com.emberone.onboarding.domain.OnboardingStepType;
import com.emberone.onboarding.payload.OnboardingStepPayloads;
import com.emberone.onboarding.service.OnboardingAttachmentPromotionService;
import com.emberone.onboarding.service.OnboardingMainOrganizationSkipEvaluator;
import com.emberone.onboarding.spi.OnboardingPromotionContext;
import com.emberone.onboarding.spi.OnboardingStepFulfillmentContext;
import com.emberone.onboarding.spi.OnboardingStepHandler;
import com.emberone.onboarding.util.OnboardingPayloadDates;
import com.emberone.platform.exception.ConflictException;
import com.emberone.trainingcourse.service.TrainingCourseRecordService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrainingCoursesStepHandler implements OnboardingStepHandler {

    private final OnboardingMainOrganizationSkipEvaluator mainOrganizationSkipEvaluator;
    private final TrainingCourseRecordService trainingCourseRecordService;
    private final OnboardingAttachmentPromotionService attachmentPromotionService;

    @Override
    public OnboardingStepType type() {
        return OnboardingStepType.TRAINING_COURSES;
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
        return mainOrganizationSkipEvaluator.hasOtherActiveMainOrganization(
                context.userId(), context.organizationId());
    }

    @Override
    public void validateOnboardingPayload(
            OnboardingStepConfig config, Map<String, Object> payload) {
        requireEmpty(config);
        for (Map<String, Object> record :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.RECORDS)) {
            if (OnboardingStepPayloads.readLongList(
                            record, OnboardingStepPayloads.TENANT_TRAINING_COURSE_TYPE_IDS)
                    .isEmpty()) {
                throw new ConflictException("Training course type is required");
            }
            if (OnboardingStepPayloads.readString(record, OnboardingStepPayloads.START_DATE)
                    .isBlank()) {
                throw new ConflictException("Training course startDate is required");
            }
            if (OnboardingStepPayloads.readString(record, OnboardingStepPayloads.END_DATE)
                    .isBlank()) {
                throw new ConflictException("Training course endDate is required");
            }
            if (OnboardingStepPayloads.readString(record, OnboardingStepPayloads.LOCATION)
                    .isBlank()) {
                throw new ConflictException("Training course location is required");
            }
        }
    }

    @Override
    public Map<String, Object> normalizeOnboardingPayload(
            OnboardingStepConfig config, Map<String, Object> payload) {
        requireEmpty(config);
        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> record :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.RECORDS)) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put(
                    OnboardingStepPayloads.TENANT_TRAINING_COURSE_TYPE_IDS,
                    OnboardingStepPayloads.readLongList(
                            record, OnboardingStepPayloads.TENANT_TRAINING_COURSE_TYPE_IDS));
            normalized.put(
                    OnboardingStepPayloads.NAME,
                    OnboardingStepPayloads.readNullableString(record, OnboardingStepPayloads.NAME));
            normalized.put(
                    OnboardingStepPayloads.START_DATE,
                    OnboardingStepPayloads.readString(record, OnboardingStepPayloads.START_DATE));
            normalized.put(
                    OnboardingStepPayloads.END_DATE,
                    OnboardingStepPayloads.readString(record, OnboardingStepPayloads.END_DATE));
            normalized.put(
                    OnboardingStepPayloads.LOCATION,
                    OnboardingStepPayloads.readString(record, OnboardingStepPayloads.LOCATION));
            if (record.get(OnboardingStepPayloads.HOURS) != null) {
                normalized.put(
                        OnboardingStepPayloads.HOURS, record.get(OnboardingStepPayloads.HOURS));
            }
            normalized.put(
                    OnboardingStepPayloads.NOTES,
                    OnboardingStepPayloads.readNullableString(
                            record, OnboardingStepPayloads.NOTES));
            String recordKey =
                    OnboardingStepPayloads.readNullableString(
                            record, OnboardingStepPayloads.RECORD_KEY);
            if (recordKey != null) {
                normalized.put(OnboardingStepPayloads.RECORD_KEY, recordKey);
            }
            records.add(normalized);
        }
        Map<String, Object> normalized = OnboardingStepPayloads.emptyPayload();
        normalized.put(OnboardingStepPayloads.RECORDS, records);
        normalized.put(
                OnboardingStepPayloads.NONE_DECLARED,
                records.isEmpty()
                        && OnboardingStepPayloads.readBoolean(
                                payload, OnboardingStepPayloads.NONE_DECLARED));
        return normalized;
    }

    @Override
    public boolean isOnboardingFulfilled(
            OnboardingStepFulfillmentContext context,
            OnboardingStepConfig config,
            Map<String, Object> payload) {
        requireEmpty(config);
        if (!OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.RECORDS)
                .isEmpty()) {
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
        for (Map<String, Object> record :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.RECORDS)) {
            Long userDocumentId =
                    attachmentPromotionService.copyDraftAttachment(
                            context.tenantId(), context.userId(), context.invitationId(), record);
            trainingCourseRecordService.createSubmissionFromOnboarding(
                    context.tenantId(),
                    context.userId(),
                    context.organizationId(),
                    OnboardingStepPayloads.readNullableString(record, OnboardingStepPayloads.NAME),
                    OnboardingStepPayloads.readLongList(
                            record, OnboardingStepPayloads.TENANT_TRAINING_COURSE_TYPE_IDS),
                    OnboardingPayloadDates.parseDate(
                            OnboardingStepPayloads.readString(
                                    record, OnboardingStepPayloads.START_DATE)),
                    OnboardingPayloadDates.parseDate(
                            OnboardingStepPayloads.readString(
                                    record, OnboardingStepPayloads.END_DATE)),
                    OnboardingStepPayloads.readString(record, OnboardingStepPayloads.LOCATION),
                    OnboardingPayloadDates.parseHours(record.get(OnboardingStepPayloads.HOURS)),
                    OnboardingStepPayloads.readNullableString(record, OnboardingStepPayloads.NOTES),
                    context.invitationId(),
                    userDocumentId);
        }
    }

    private static void requireEmpty(OnboardingStepConfig config) {
        if (!(config instanceof EmptyStepConfig)) {
            throw new IllegalArgumentException("Expected empty step config");
        }
    }
}
