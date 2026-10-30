/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.handler;

import com.emberone.certification.service.CertificationAssignmentService;
import com.emberone.certification.service.CertificationRecordService;
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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CertificationsStepHandler implements OnboardingStepHandler {

    private final OnboardingMainOrganizationSkipEvaluator mainOrganizationSkipEvaluator;
    private final CertificationAssignmentService certificationAssignmentService;
    private final CertificationRecordService certificationRecordService;
    private final OnboardingAttachmentPromotionService attachmentPromotionService;

    @Override
    public OnboardingStepType type() {
        return OnboardingStepType.CERTIFICATIONS;
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
            Long definitionId =
                    OnboardingStepPayloads.readLong(
                            record, OnboardingStepPayloads.TENANT_CERT_DEFINITION_ID);
            if (definitionId == null) {
                throw new ConflictException("Certification definition is required");
            }
            if (OnboardingStepPayloads.readString(record, OnboardingStepPayloads.COMPLETED_DATE)
                    .isBlank()) {
                throw new ConflictException("Certification completedDate is required");
            }
            certificationRecordService.validateExpiryForCapture(
                    definitionId,
                    parseDate(record, OnboardingStepPayloads.COMPLETED_DATE),
                    parseDate(record, OnboardingStepPayloads.EXPIRY_DATE));
        }
    }

    private static LocalDate parseDate(Map<String, Object> record, String key) {
        try {
            return OnboardingPayloadDates.parseDate(
                    OnboardingStepPayloads.readNullableString(record, key));
        } catch (DateTimeParseException ex) {
            throw new ConflictException("Invalid certification date: " + key);
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
                    OnboardingStepPayloads.TENANT_CERT_DEFINITION_ID,
                    OnboardingStepPayloads.readLong(
                            record, OnboardingStepPayloads.TENANT_CERT_DEFINITION_ID));
            normalized.put(
                    OnboardingStepPayloads.COMPLETED_DATE,
                    OnboardingStepPayloads.readString(
                            record, OnboardingStepPayloads.COMPLETED_DATE));
            normalized.put(
                    OnboardingStepPayloads.EXPIRY_DATE,
                    OnboardingStepPayloads.readNullableString(
                            record, OnboardingStepPayloads.EXPIRY_DATE));
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
        List<Map<String, Object>> records =
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.RECORDS);
        List<Long> definitionIds = new ArrayList<>();
        for (Map<String, Object> record : records) {
            Long definitionId =
                    OnboardingStepPayloads.readLong(
                            record, OnboardingStepPayloads.TENANT_CERT_DEFINITION_ID);
            if (definitionId != null) {
                definitionIds.add(definitionId);
            }
        }
        certificationAssignmentService.createAssignmentsFromOnboarding(
                context.tenantId(),
                context.userId(),
                context.organizationId(),
                context.invitationId(),
                definitionIds);
        for (Map<String, Object> record : records) {
            Long definitionId =
                    OnboardingStepPayloads.readLong(
                            record, OnboardingStepPayloads.TENANT_CERT_DEFINITION_ID);
            Long userDocumentId =
                    attachmentPromotionService.copyDraftAttachment(
                            context.tenantId(), context.userId(), context.invitationId(), record);
            certificationRecordService.createFromOnboarding(
                    context.tenantId(),
                    context.userId(),
                    definitionId,
                    OnboardingPayloadDates.parseDate(
                            OnboardingStepPayloads.readString(
                                    record, OnboardingStepPayloads.COMPLETED_DATE)),
                    OnboardingPayloadDates.parseDate(
                            OnboardingStepPayloads.readNullableString(
                                    record, OnboardingStepPayloads.EXPIRY_DATE)),
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
