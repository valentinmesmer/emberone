/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.handler;

import com.emberone.onboarding.config.EntryDocumentsStepConfig;
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
import com.emberone.onboardingdocument.service.OnboardingDocumentTemplateService;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinition;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinitionRepository;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.exception.ConflictException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EntryDocumentsStepHandler implements OnboardingStepHandler {

    public static final String DOCUMENT_CONFIG_KEYS = "documentConfigKeys";

    private final TenantOnboardingDocumentDefinitionRepository definitionRepository;
    private final OnboardingDocumentTemplateService documentTemplateService;
    private final OnboardingEntryDocumentReuseService documentReuseService;
    private final OnboardingEntryDocumentDraftSupport documentDraftSupport;
    private final OnboardingAttachmentPromotionService attachmentPromotionService;

    @Override
    public OnboardingStepType type() {
        return OnboardingStepType.ENTRY_DOCUMENTS;
    }

    @Override
    public OnboardingStepConfig parseConfig(Map<String, Object> raw) {
        return new EntryDocumentsStepConfig(readStringList(raw.get(DOCUMENT_CONFIG_KEYS)));
    }

    @Override
    public void validateConfig(long tenantId, OnboardingStepConfig config) {
        EntryDocumentsStepConfig typed = requireType(config);
        Set<String> uniqueKeys = new LinkedHashSet<>();
        for (String configKey : typed.documentConfigKeys()) {
            if (configKey == null || configKey.isBlank()) {
                continue;
            }
            String trimmed = configKey.trim();
            TenantOnboardingDocumentDefinition definition =
                    definitionRepository
                            .findByConfigKey(trimmed)
                            .orElseThrow(
                                    () ->
                                            new ConflictException(
                                                    "Unknown tenant onboarding document: "
                                                            + trimmed));
            if (!LifecycleFields.isCatalogSelectable(definition)) {
                if (definition.isArchived()) {
                    throw new ConflictException(
                            "Tenant onboarding document is archived: " + trimmed);
                }
                throw new ConflictException("Tenant onboarding document is not active: " + trimmed);
            }
            if (!documentTemplateService.hasCurrentTemplate(definition)) {
                throw new ConflictException(
                        "Tenant onboarding document has no template file: " + trimmed);
            }
            uniqueKeys.add(trimmed);
        }
        if (uniqueKeys.isEmpty()) {
            throw new ConflictException("Entry documents step requires at least one document");
        }
    }

    @Override
    public Map<String, Object> toConfigMap(OnboardingStepConfig config) {
        EntryDocumentsStepConfig typed = requireType(config);
        return Map.of(DOCUMENT_CONFIG_KEYS, typed.documentConfigKeys());
    }

    @Override
    public boolean isFulfilled(
            OnboardingStepFulfillmentContext context, OnboardingStepConfig config) {
        EntryDocumentsStepConfig typed = requireType(config);
        return documentReuseService.areAllTenantReusable(context, typed.documentConfigKeys());
    }

    @Override
    public void validateOnboardingPayload(
            OnboardingStepConfig config, Map<String, Object> payload) {
        EntryDocumentsStepConfig typed = requireType(config);
        Set<String> allowed = new LinkedHashSet<>(typed.documentConfigKeys());
        for (Map<String, Object> document :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.DOCUMENTS)) {
            String configKey =
                    OnboardingStepPayloads.readString(document, OnboardingStepPayloads.CONFIG_KEY);
            if (configKey.isBlank() || !allowed.contains(configKey)) {
                throw new ConflictException("Unknown onboarding document: " + configKey);
            }
        }
    }

    @Override
    public Map<String, Object> normalizeOnboardingPayload(
            OnboardingStepConfig config, Map<String, Object> payload) {
        EntryDocumentsStepConfig typed = requireType(config);
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
        for (String configKey : typed.documentConfigKeys()) {
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
        requireType(config);
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
        EntryDocumentsStepConfig typed = requireType(config);
        if (typed.documentConfigKeys().isEmpty()) {
            return false;
        }
        Set<String> reusable =
                documentReuseService.findReusableTenantConfigKeys(
                        context, typed.documentConfigKeys());
        Set<String> declined =
                documentReuseService.findDeclinedTenantConfigKeys(
                        context, typed.documentConfigKeys());
        Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
        for (Map<String, Object> document :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.DOCUMENTS)) {
            String configKey =
                    OnboardingStepPayloads.readString(document, OnboardingStepPayloads.CONFIG_KEY);
            if (!configKey.isBlank()) {
                byKey.put(configKey, document);
            }
        }
        for (String configKey : typed.documentConfigKeys()) {
            if (reusable.contains(configKey) || declined.contains(configKey)) {
                continue;
            }
            TenantOnboardingDocumentDefinition definition =
                    definitionRepository.findByConfigKey(configKey).orElse(null);
            if (definition == null || !LifecycleFields.isCatalogSelectable(definition)) {
                return false;
            }
            if (!definition.isMandatory()) {
                continue;
            }
            if (!documentDraftSupport.isTenantDocumentFulfilled(byKey.get(configKey), definition)) {
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
        EntryDocumentsStepConfig typed = requireType(config);
        OnboardingStepFulfillmentContext fulfillmentContext =
                new OnboardingStepFulfillmentContext(
                        context.tenantId(),
                        context.userId(),
                        context.organizationId(),
                        context.invitationId());
        if (isFulfilled(fulfillmentContext, config) && !hasInvitationWork(config, payload)) {
            return;
        }
        Set<String> reusable =
                documentReuseService.findReusableTenantConfigKeys(
                        fulfillmentContext, typed.documentConfigKeys());
        Set<String> declined =
                documentReuseService.findDeclinedTenantConfigKeys(
                        fulfillmentContext, typed.documentConfigKeys());
        Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
        for (Map<String, Object> document :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.DOCUMENTS)) {
            String configKey =
                    OnboardingStepPayloads.readString(document, OnboardingStepPayloads.CONFIG_KEY);
            if (!configKey.isBlank()) {
                byKey.put(configKey, document);
            }
        }
        for (String configKey : typed.documentConfigKeys()) {
            if (reusable.contains(configKey) || declined.contains(configKey)) {
                continue;
            }
            TenantOnboardingDocumentDefinition definition =
                    definitionRepository
                            .findByConfigKey(configKey)
                            .orElseThrow(
                                    () ->
                                            new ConflictException(
                                                    "Unknown tenant onboarding document: "
                                                            + configKey));
            Map<String, Object> document = byKey.get(configKey);
            if (!definition.isMandatory()) {
                if (documentDraftSupport.isTenantDocumentDeclined(document, definition)) {
                    attachmentPromotionService.recordDeclinedTenantEntryDocument(
                            context.tenantId(), context.userId(), configKey);
                    continue;
                }
                if (!documentDraftSupport.isTenantDocumentFulfilled(document, definition)) {
                    continue;
                }
            } else if (!documentDraftSupport.isTenantDocumentFulfilled(document, definition)) {
                throw new ConflictException(
                        "Onboarding entry document is not fulfilled: " + configKey);
            }
            if (definition.getFulfillmentMode()
                    == OnboardingDocumentFulfillmentMode.SIGNED_UPLOAD) {
                attachmentPromotionService.upsertSignedEntryDocument(
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
                attachmentPromotionService.upsertConfirmedEntryDocument(
                        context, configKey, document);
            }
        }
    }

    private static void copyIfPresent(
            Map<String, Object> source, Map<String, Object> target, String key) {
        Object value = source.get(key);
        if (value != null && !(value instanceof String text && text.isBlank())) {
            target.put(key, value);
        }
    }

    private static EntryDocumentsStepConfig requireType(OnboardingStepConfig config) {
        if (!(config instanceof EntryDocumentsStepConfig typed)) {
            throw new ConflictException("Expected entry documents step config");
        }
        return typed;
    }

    @SuppressWarnings("unchecked")
    private static List<String> readStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }
        return List.of();
    }
}
