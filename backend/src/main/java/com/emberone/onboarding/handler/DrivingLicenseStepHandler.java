/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.handler;

import com.emberone.drivinglicense.dto.DrivingLicenseClassInput;
import com.emberone.drivinglicense.service.DrivingLicenseRecordService;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrivingLicenseStepHandler implements OnboardingStepHandler {

    private final OnboardingMainOrganizationSkipEvaluator mainOrganizationSkipEvaluator;
    private final DrivingLicenseRecordService drivingLicenseRecordService;
    private final OnboardingAttachmentPromotionService attachmentPromotionService;

    @Override
    public OnboardingStepType type() {
        return OnboardingStepType.DRIVING_LICENSE;
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
        if (!OnboardingStepPayloads.hasKey(payload, OnboardingStepPayloads.LICENSE)) {
            return;
        }
        Map<String, Object> license =
                OnboardingStepPayloads.readObject(payload, OnboardingStepPayloads.LICENSE);
        if (license == null) {
            return;
        }
        if (OnboardingStepPayloads.readString(license, OnboardingStepPayloads.ISSUE_DATE)
                .isBlank()) {
            throw new ConflictException("Driving license issueDate is required");
        }
        if (OnboardingStepPayloads.readString(license, OnboardingStepPayloads.LICENSE_NUMBER)
                .isBlank()) {
            throw new ConflictException("Driving license number is required");
        }
        if (OnboardingStepPayloads.readString(license, OnboardingStepPayloads.ISSUING_AUTHORITY)
                .isBlank()) {
            throw new ConflictException("Driving license issuing authority is required");
        }
        if (OnboardingStepPayloads.readObjectList(license, OnboardingStepPayloads.CLASSES)
                .isEmpty()) {
            throw new ConflictException("Driving license requires at least one class");
        }
        for (Map<String, Object> licenseClass :
                OnboardingStepPayloads.readObjectList(license, OnboardingStepPayloads.CLASSES)) {
            if (OnboardingStepPayloads.readLong(
                            licenseClass,
                            OnboardingStepPayloads.TENANT_DRIVING_LICENSE_DEFINITION_ID)
                    == null) {
                throw new ConflictException("Driving license class is required");
            }
        }
    }

    @Override
    public Map<String, Object> normalizeOnboardingPayload(
            OnboardingStepConfig config, Map<String, Object> payload) {
        requireEmpty(config);
        Map<String, Object> normalized = OnboardingStepPayloads.emptyPayload();
        Map<String, Object> license =
                OnboardingStepPayloads.readObject(payload, OnboardingStepPayloads.LICENSE);
        boolean noneDeclared =
                OnboardingStepPayloads.readBoolean(payload, OnboardingStepPayloads.NONE_DECLARED);
        if (license == null) {
            normalized.put(OnboardingStepPayloads.LICENSE, null);
            normalized.put(OnboardingStepPayloads.NONE_DECLARED, noneDeclared);
            return normalized;
        }
        Map<String, Object> licenseMap = new LinkedHashMap<>();
        licenseMap.put(
                OnboardingStepPayloads.ISSUE_DATE,
                OnboardingStepPayloads.readString(license, OnboardingStepPayloads.ISSUE_DATE));
        licenseMap.put(
                OnboardingStepPayloads.DOCUMENT_EXPIRATION_DATE,
                OnboardingStepPayloads.readNullableString(
                        license, OnboardingStepPayloads.DOCUMENT_EXPIRATION_DATE));
        licenseMap.put(
                OnboardingStepPayloads.LICENSE_NUMBER,
                OnboardingStepPayloads.readString(license, OnboardingStepPayloads.LICENSE_NUMBER));
        licenseMap.put(
                OnboardingStepPayloads.ISSUING_AUTHORITY,
                OnboardingStepPayloads.readString(
                        license, OnboardingStepPayloads.ISSUING_AUTHORITY));
        licenseMap.put(
                OnboardingStepPayloads.NOTES,
                OnboardingStepPayloads.readNullableString(license, OnboardingStepPayloads.NOTES));
        List<Map<String, Object>> classes = new ArrayList<>();
        for (Map<String, Object> licenseClass :
                OnboardingStepPayloads.readObjectList(license, OnboardingStepPayloads.CLASSES)) {
            Map<String, Object> classMap = new LinkedHashMap<>();
            classMap.put(
                    OnboardingStepPayloads.TENANT_DRIVING_LICENSE_DEFINITION_ID,
                    OnboardingStepPayloads.readLong(
                            licenseClass,
                            OnboardingStepPayloads.TENANT_DRIVING_LICENSE_DEFINITION_ID));
            classMap.put(
                    OnboardingStepPayloads.GRANT_DATE,
                    OnboardingStepPayloads.readNullableString(
                            licenseClass, OnboardingStepPayloads.GRANT_DATE));
            classMap.put(
                    OnboardingStepPayloads.EXPIRATION_DATE,
                    OnboardingStepPayloads.readNullableString(
                            licenseClass, OnboardingStepPayloads.EXPIRATION_DATE));
            classes.add(classMap);
        }
        licenseMap.put(OnboardingStepPayloads.CLASSES, classes);
        normalized.put(OnboardingStepPayloads.LICENSE, licenseMap);
        normalized.put(OnboardingStepPayloads.NONE_DECLARED, false);
        return normalized;
    }

    @Override
    public boolean isOnboardingFulfilled(
            OnboardingStepFulfillmentContext context,
            OnboardingStepConfig config,
            Map<String, Object> payload) {
        requireEmpty(config);
        if (OnboardingStepPayloads.readObject(payload, OnboardingStepPayloads.LICENSE) != null) {
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
        Map<String, Object> license =
                OnboardingStepPayloads.readObject(payload, OnboardingStepPayloads.LICENSE);
        if (license == null) {
            return;
        }
        List<DrivingLicenseClassInput> classes = new ArrayList<>();
        for (Map<String, Object> licenseClass :
                OnboardingStepPayloads.readObjectList(license, OnboardingStepPayloads.CLASSES)) {
            DrivingLicenseClassInput input = new DrivingLicenseClassInput();
            input.setTenantDrivingLicenseDefinitionId(
                    OnboardingStepPayloads.readLong(
                            licenseClass,
                            OnboardingStepPayloads.TENANT_DRIVING_LICENSE_DEFINITION_ID));
            input.setGrantDate(
                    OnboardingPayloadDates.parseDate(
                            OnboardingStepPayloads.readNullableString(
                                    licenseClass, OnboardingStepPayloads.GRANT_DATE)));
            input.setExpirationDate(
                    OnboardingPayloadDates.parseDate(
                            OnboardingStepPayloads.readNullableString(
                                    licenseClass, OnboardingStepPayloads.EXPIRATION_DATE)));
            classes.add(input);
        }
        Long userDocumentId =
                attachmentPromotionService.copyDraftAttachment(
                        context.tenantId(), context.userId(), context.invitationId(), license);
        drivingLicenseRecordService.upsertFromOnboarding(
                context.tenantId(),
                context.userId(),
                context.organizationId(),
                OnboardingPayloadDates.parseDate(
                        OnboardingStepPayloads.readString(
                                license, OnboardingStepPayloads.ISSUE_DATE)),
                OnboardingPayloadDates.parseDate(
                        OnboardingStepPayloads.readNullableString(
                                license, OnboardingStepPayloads.DOCUMENT_EXPIRATION_DATE)),
                OnboardingStepPayloads.readString(license, OnboardingStepPayloads.LICENSE_NUMBER),
                OnboardingStepPayloads.readString(
                        license, OnboardingStepPayloads.ISSUING_AUTHORITY),
                OnboardingStepPayloads.readNullableString(license, OnboardingStepPayloads.NOTES),
                classes,
                context.invitationId(),
                userDocumentId);
    }

    private static void requireEmpty(OnboardingStepConfig config) {
        if (!(config instanceof EmptyStepConfig)) {
            throw new IllegalArgumentException("Expected empty step config");
        }
    }
}
