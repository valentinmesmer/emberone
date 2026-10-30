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
import com.emberone.onboarding.service.OnboardingEntryDocumentDraftSupport;
import com.emberone.onboarding.service.OnboardingEntryDocumentReuseService;
import com.emberone.onboarding.spi.OnboardingPromotionContext;
import com.emberone.onboarding.spi.OnboardingStepFulfillmentContext;
import com.emberone.onboarding.spi.OnboardingStepHandler;
import com.emberone.onboardingdocument.domain.OnboardingDocumentFulfillmentMode;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinition;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinitionRepository;
import com.emberone.onboardingdocument.service.OrganizationOnboardingDocumentDefinitionService;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.exception.ConflictException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganizationEntryDocumentsStepHandler implements OnboardingStepHandler {

    private final OrganizationOnboardingDocumentDefinitionService organizationDocumentService;
    private final OrganizationOnboardingDocumentDefinitionRepository definitionRepository;
    private final OnboardingEntryDocumentReuseService documentReuseService;
    private final OnboardingEntryDocumentDraftSupport documentDraftSupport;
    private final OnboardingAttachmentPromotionService attachmentPromotionService;

    @Override
    public OnboardingStepType type() {
        return OnboardingStepType.ORGANIZATION_ENTRY_DOCUMENTS;
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
        List<String> configKeys = resolveConfigKeys(context);
        if (configKeys.isEmpty()) {
            return true;
        }
        return documentReuseService.areAllOrganizationReusable(context, configKeys);
    }

    @Override
    public void validateOnboardingPayload(
            OnboardingStepConfig config, Map<String, Object> payload) {
        requireEmpty(config);
    }

    @Override
    public Map<String, Object> normalizeOnboardingPayload(
            OnboardingStepConfig config, Map<String, Object> payload) {
        requireEmpty(config);
        return payload == null ? Map.of() : payload;
    }

    public Map<String, Object> normalizeOnboardingPayload(
            OnboardingStepFulfillmentContext context,
            OnboardingStepConfig config,
            Map<String, Object> payload) {
        requireEmpty(config);
        List<String> configKeys = resolveConfigKeys(context);
        Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
        for (Map<String, Object> document :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.DOCUMENTS)) {
            String configKey =
                    OnboardingStepPayloads.readString(document, OnboardingStepPayloads.CONFIG_KEY);
            if (configKey.isBlank()) {
                continue;
            }
            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put(OnboardingStepPayloads.CONFIG_KEY, configKey);
            normalized.put(
                    OnboardingStepPayloads.CONFIRMED,
                    OnboardingStepPayloads.readBoolean(document, OnboardingStepPayloads.CONFIRMED));
            normalized.put(
                    OnboardingStepPayloads.DECLINED,
                    OnboardingStepPayloads.readBoolean(document, OnboardingStepPayloads.DECLINED));
            copyIfPresent(document, normalized, OnboardingStepPayloads.FILE_NAME);
            copyIfPresent(document, normalized, OnboardingStepPayloads.STORAGE_KEY);
            copyIfPresent(document, normalized, OnboardingStepPayloads.CONTENT_TYPE);
            copyIfPresent(document, normalized, OnboardingStepPayloads.SIZE_BYTES);
            copyIfPresent(document, normalized, OnboardingStepPayloads.DOCUMENT_VERSION);
            byKey.put(configKey, normalized);
        }
        List<Map<String, Object>> documents = new ArrayList<>();
        for (String configKey : configKeys) {
            documents.add(
                    byKey.getOrDefault(
                            configKey,
                            Map.of(
                                    OnboardingStepPayloads.CONFIG_KEY,
                                    configKey,
                                    OnboardingStepPayloads.CONFIRMED,
                                    false,
                                    OnboardingStepPayloads.DECLINED,
                                    false)));
        }
        Map<String, Object> normalized = OnboardingStepPayloads.emptyPayload();
        normalized.put(OnboardingStepPayloads.DOCUMENTS, documents);
        return normalized;
    }

    @Override
    public boolean hasInvitationWork(OnboardingStepConfig config, Map<String, Object> payload) {
        requireEmpty(config);
        for (Map<String, Object> document :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.DOCUMENTS)) {
            if (OnboardingStepPayloads.readBoolean(document, OnboardingStepPayloads.CONFIRMED)
                    || OnboardingStepPayloads.readBoolean(document, OnboardingStepPayloads.DECLINED)
                    || OnboardingStepPayloads.readNullableString(
                                    document, OnboardingStepPayloads.STORAGE_KEY)
                            != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isOnboardingFulfilled(
            OnboardingStepFulfillmentContext context,
            OnboardingStepConfig config,
            Map<String, Object> payload) {
        requireEmpty(config);
        List<String> configKeys = resolveConfigKeys(context);
        if (configKeys.isEmpty()) {
            return true;
        }
        Set<String> reusable =
                documentReuseService.findReusableOrganizationConfigKeys(context, configKeys);
        Set<String> declined =
                documentReuseService.findDeclinedOrganizationConfigKeys(context, configKeys);
        Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
        for (Map<String, Object> document :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.DOCUMENTS)) {
            String configKey =
                    OnboardingStepPayloads.readString(document, OnboardingStepPayloads.CONFIG_KEY);
            if (!configKey.isBlank()) {
                byKey.put(configKey, document);
            }
        }
        for (String configKey : configKeys) {
            if (reusable.contains(configKey) || declined.contains(configKey)) {
                continue;
            }
            OrganizationOnboardingDocumentDefinition definition =
                    definitionRepository
                            .findByOrganizationIdAndConfigKey(context.organizationId(), configKey)
                            .orElse(null);
            if (definition == null || !LifecycleFields.isCatalogSelectable(definition)) {
                return false;
            }
            if (!definition.isMandatory()) {
                continue;
            }
            if (!documentDraftSupport.isOrganizationDocumentFulfilled(
                    byKey.get(configKey), definition)) {
                return false;
            }
        }
        return true;
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
        if (isFulfilled(fulfillmentContext, config) && !hasInvitationWork(config, payload)) {
            return;
        }
        List<String> configKeys = resolveConfigKeys(fulfillmentContext);
        Set<String> reusable =
                documentReuseService.findReusableOrganizationConfigKeys(
                        fulfillmentContext, configKeys);
        Set<String> declined =
                documentReuseService.findDeclinedOrganizationConfigKeys(
                        fulfillmentContext, configKeys);
        Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
        for (Map<String, Object> document :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.DOCUMENTS)) {
            String configKey =
                    OnboardingStepPayloads.readString(document, OnboardingStepPayloads.CONFIG_KEY);
            if (!configKey.isBlank()) {
                byKey.put(configKey, document);
            }
        }
        for (String configKey : configKeys) {
            if (reusable.contains(configKey) || declined.contains(configKey)) {
                continue;
            }
            OrganizationOnboardingDocumentDefinition definition =
                    definitionRepository
                            .findByOrganizationIdAndConfigKey(context.organizationId(), configKey)
                            .orElseThrow(
                                    () ->
                                            new ConflictException(
                                                    "Unknown organization onboarding document: "
                                                            + configKey));
            Map<String, Object> document = byKey.get(configKey);
            if (!definition.isMandatory()) {
                if (documentDraftSupport.isOrganizationDocumentDeclined(document, definition)) {
                    attachmentPromotionService.recordDeclinedOrganizationEntryDocument(
                            context.tenantId(),
                            context.organizationId(),
                            context.userId(),
                            configKey);
                    continue;
                }
                if (!documentDraftSupport.isOrganizationDocumentFulfilled(document, definition)) {
                    continue;
                }
            } else if (!documentDraftSupport.isOrganizationDocumentFulfilled(
                    document, definition)) {
                throw new ConflictException(
                        "Onboarding entry document is not fulfilled: " + configKey);
            }
            if (definition.getFulfillmentMode()
                    == OnboardingDocumentFulfillmentMode.SIGNED_UPLOAD) {
                attachmentPromotionService.upsertSignedOrganizationEntryDocument(
                        context,
                        configKey,
                        document,
                        OnboardingStepPayloads.readNullableString(
                                document, OnboardingStepPayloads.STORAGE_KEY),
                        OnboardingStepPayloads.readNullableString(
                                document, OnboardingStepPayloads.FILE_NAME),
                        OnboardingStepPayloads.readNullableString(
                                document, OnboardingStepPayloads.CONTENT_TYPE),
                        OnboardingStepPayloads.readLong(
                                document, OnboardingStepPayloads.SIZE_BYTES),
                        definition.getName());
            } else {
                attachmentPromotionService.upsertConfirmedOrganizationEntryDocument(
                        context, configKey, document);
            }
        }
    }

    public List<String> resolveConfigKeys(OnboardingStepFulfillmentContext context) {
        return organizationDocumentService.listActiveConfigKeys(context.organizationId());
    }

    private static void copyIfPresent(
            Map<String, Object> source, Map<String, Object> target, String key) {
        Object value = source.get(key);
        if (value != null && !(value instanceof String text && text.isBlank())) {
            target.put(key, value);
        }
    }

    private static void requireEmpty(OnboardingStepConfig config) {
        if (!(config instanceof EmptyStepConfig)) {
            throw new ConflictException("Expected organization entry documents step config");
        }
    }
}
