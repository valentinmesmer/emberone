/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.service;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.onboarding.payload.OnboardingStepPayloads;
import com.emberone.onboardingdocument.domain.OnboardingDocumentFulfillmentMode;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinition;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinitionRepository;
import com.emberone.onboardingdocument.service.OnboardingDocumentTemplateService;
import com.emberone.onboardingdocument.service.OrganizationOnboardingDocumentTemplateService;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinition;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinitionRepository;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.i18n.LocalizationService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingEntryDocumentDraftSupport {

    private static final SupportedLocale LOCALE = SupportedLocale.DE;

    private static final DateTimeFormatter CONFIRMATION_DOCUMENT_DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.of("Europe/Berlin"));

    private final TenantOnboardingDocumentDefinitionRepository tenantDefinitionRepository;
    private final OrganizationOnboardingDocumentDefinitionRepository orgDefinitionRepository;
    private final OnboardingDocumentTemplateService tenantTemplateService;
    private final OrganizationOnboardingDocumentTemplateService organizationTemplateService;
    private final LocalizationService localizationService;

    public Map<String, Object> stampTenantDocumentVersions(Map<String, Object> payload) {
        List<Map<String, Object>> documents = new ArrayList<>();
        for (Map<String, Object> document :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.DOCUMENTS)) {
            Map<String, Object> next = new LinkedHashMap<>(document);
            String configKey =
                    OnboardingStepPayloads.readString(next, OnboardingStepPayloads.CONFIG_KEY);
            tenantDefinitionRepository
                    .findByConfigKey(configKey)
                    .ifPresent(
                            definition -> stampVersion(next, definition.getVersion(), definition));
            documents.add(next);
        }
        Map<String, Object> stamped = OnboardingStepPayloads.emptyPayload();
        stamped.put(OnboardingStepPayloads.DOCUMENTS, documents);
        return stamped;
    }

    public Map<String, Object> stampOrganizationDocumentVersions(
            long organizationId, Map<String, Object> payload) {
        List<Map<String, Object>> documents = new ArrayList<>();
        for (Map<String, Object> document :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.DOCUMENTS)) {
            Map<String, Object> next = new LinkedHashMap<>(document);
            String configKey =
                    OnboardingStepPayloads.readString(next, OnboardingStepPayloads.CONFIG_KEY);
            orgDefinitionRepository
                    .findByOrganizationIdAndConfigKey(organizationId, configKey)
                    .ifPresent(
                            definition -> stampVersion(next, definition.getVersion(), definition));
            documents.add(next);
        }
        Map<String, Object> stamped = OnboardingStepPayloads.emptyPayload();
        stamped.put(OnboardingStepPayloads.DOCUMENTS, documents);
        return stamped;
    }

    public Map<String, Object> finalizeTenantEntryDocumentPayload(
            Map<String, Object> payload, Map<String, Object> previousPayload) {
        List<Map<String, Object>> documents = new ArrayList<>();
        Map<String, Map<String, Object>> previousByKey = documentsByKey(previousPayload);
        for (Map<String, Object> document :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.DOCUMENTS)) {
            String configKey =
                    OnboardingStepPayloads.readString(document, OnboardingStepPayloads.CONFIG_KEY);
            TenantOnboardingDocumentDefinition definition =
                    tenantDefinitionRepository.findByConfigKey(configKey).orElse(null);
            documents.add(
                    finalizeDocument(
                            document,
                            previousByKey.get(configKey),
                            definition == null ? null : definition.getVersion(),
                            definition));
        }
        Map<String, Object> finalized = new LinkedHashMap<>(payload);
        finalized.put(OnboardingStepPayloads.DOCUMENTS, documents);
        return finalized;
    }

    public Map<String, Object> finalizeOrganizationEntryDocumentPayload(
            long organizationId, Map<String, Object> payload, Map<String, Object> previousPayload) {
        List<Map<String, Object>> documents = new ArrayList<>();
        Map<String, Map<String, Object>> previousByKey = documentsByKey(previousPayload);
        for (Map<String, Object> document :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.DOCUMENTS)) {
            String configKey =
                    OnboardingStepPayloads.readString(document, OnboardingStepPayloads.CONFIG_KEY);
            OrganizationOnboardingDocumentDefinition definition =
                    orgDefinitionRepository
                            .findByOrganizationIdAndConfigKey(organizationId, configKey)
                            .orElse(null);
            documents.add(
                    finalizeDocument(
                            document,
                            previousByKey.get(configKey),
                            definition == null ? null : definition.getVersion(),
                            definition));
        }
        Map<String, Object> finalized = new LinkedHashMap<>(payload);
        finalized.put(OnboardingStepPayloads.DOCUMENTS, documents);
        return finalized;
    }

    public void validateTenantDocuments(Map<String, Object> payload) {
        for (Map<String, Object> document :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.DOCUMENTS)) {
            String configKey =
                    OnboardingStepPayloads.readString(document, OnboardingStepPayloads.CONFIG_KEY);
            tenantDefinitionRepository
                    .findByConfigKey(configKey)
                    .ifPresent(definition -> validateDocument(document, definition));
        }
    }

    public void validateOrganizationDocuments(long organizationId, Map<String, Object> payload) {
        for (Map<String, Object> document :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.DOCUMENTS)) {
            String configKey =
                    OnboardingStepPayloads.readString(document, OnboardingStepPayloads.CONFIG_KEY);
            orgDefinitionRepository
                    .findByOrganizationIdAndConfigKey(organizationId, configKey)
                    .ifPresent(definition -> validateDocument(document, definition));
        }
    }

    public void requireTemplateOpenedBeforeSignedUpload(
            TenantOnboardingDocumentDefinition definition, Map<String, Object> document) {
        requireTemplateOpened(
                definition, document, tenantTemplateService.hasCurrentTemplate(definition));
    }

    public void requireTemplateOpenedBeforeSignedUpload(
            OrganizationOnboardingDocumentDefinition definition, Map<String, Object> document) {
        requireTemplateOpened(
                definition, document, organizationTemplateService.hasCurrentTemplate(definition));
    }

    public boolean isTenantDocumentFulfilled(
            Map<String, Object> document, TenantOnboardingDocumentDefinition definition) {
        if (document == null
                || definition == null
                || !LifecycleFields.isCatalogSelectable(definition)) {
            return false;
        }
        Integer storedVersion =
                OnboardingStepPayloads.readInteger(
                        document, OnboardingStepPayloads.DOCUMENT_VERSION);
        if (storedVersion == null || storedVersion != definition.getVersion()) {
            return false;
        }
        if (definition.getFulfillmentMode() == OnboardingDocumentFulfillmentMode.SIGNED_UPLOAD) {
            return OnboardingStepPayloads.readNullableString(
                            document, OnboardingStepPayloads.STORAGE_KEY)
                    != null;
        }
        return OnboardingStepPayloads.readBoolean(document, OnboardingStepPayloads.CONFIRMED);
    }

    public boolean isTenantDocumentDeclined(
            Map<String, Object> document, TenantOnboardingDocumentDefinition definition) {
        if (document == null
                || definition == null
                || definition.isMandatory()
                || !LifecycleFields.isCatalogSelectable(definition)) {
            return false;
        }
        Integer storedVersion =
                OnboardingStepPayloads.readInteger(
                        document, OnboardingStepPayloads.DOCUMENT_VERSION);
        if (storedVersion == null || storedVersion != definition.getVersion()) {
            return false;
        }
        return OnboardingStepPayloads.readBoolean(document, OnboardingStepPayloads.DECLINED);
    }

    public boolean isOrganizationDocumentFulfilled(
            Map<String, Object> document, OrganizationOnboardingDocumentDefinition definition) {
        if (document == null
                || definition == null
                || !LifecycleFields.isCatalogSelectable(definition)) {
            return false;
        }
        Integer storedVersion =
                OnboardingStepPayloads.readInteger(
                        document, OnboardingStepPayloads.DOCUMENT_VERSION);
        if (storedVersion == null || storedVersion != definition.getVersion()) {
            return false;
        }
        if (definition.getFulfillmentMode() == OnboardingDocumentFulfillmentMode.SIGNED_UPLOAD) {
            return OnboardingStepPayloads.readNullableString(
                            document, OnboardingStepPayloads.STORAGE_KEY)
                    != null;
        }
        return OnboardingStepPayloads.readBoolean(document, OnboardingStepPayloads.CONFIRMED);
    }

    public boolean isOrganizationDocumentDeclined(
            Map<String, Object> document, OrganizationOnboardingDocumentDefinition definition) {
        if (document == null
                || definition == null
                || definition.isMandatory()
                || !LifecycleFields.isCatalogSelectable(definition)) {
            return false;
        }
        Integer storedVersion =
                OnboardingStepPayloads.readInteger(
                        document, OnboardingStepPayloads.DOCUMENT_VERSION);
        if (storedVersion == null || storedVersion != definition.getVersion()) {
            return false;
        }
        return OnboardingStepPayloads.readBoolean(document, OnboardingStepPayloads.DECLINED);
    }

    private Map<String, Object> finalizeDocument(
            Map<String, Object> document,
            Map<String, Object> previous,
            Integer currentVersion,
            TenantOnboardingDocumentDefinition definition) {
        Map<String, Object> next = new LinkedHashMap<>(document);
        mergeOpenedAt(next, previous);
        if (currentVersion != null
                && !OnboardingStepPayloads.readBoolean(next, OnboardingStepPayloads.CONFIRMED)
                && OnboardingStepPayloads.readNullableString(
                                next, OnboardingStepPayloads.STORAGE_KEY)
                        == null) {
            Integer storedVersion =
                    OnboardingStepPayloads.readInteger(
                            next, OnboardingStepPayloads.DOCUMENT_VERSION);
            if (storedVersion != null && storedVersion != currentVersion) {
                next.remove(OnboardingStepPayloads.OPENED_AT);
            }
        }
        if (OnboardingStepPayloads.readBoolean(next, OnboardingStepPayloads.CONFIRMED)) {
            boolean wasConfirmed =
                    previous != null
                            && OnboardingStepPayloads.readBoolean(
                                    previous, OnboardingStepPayloads.CONFIRMED);
            if (!wasConfirmed) {
                next.put(OnboardingStepPayloads.CONFIRMED_AT, Instant.now().toString());
            } else {
                copyIfPresent(previous, next, OnboardingStepPayloads.CONFIRMED_AT);
            }
        } else {
            next.remove(OnboardingStepPayloads.CONFIRMATION_TEXT);
            next.remove(OnboardingStepPayloads.CONFIRMED_AT);
        }
        if (definition != null) {
            stampTenantConfirmationText(next, definition);
            validateDocument(next, definition);
        }
        return next;
    }

    private Map<String, Object> finalizeDocument(
            Map<String, Object> document,
            Map<String, Object> previous,
            Integer currentVersion,
            OrganizationOnboardingDocumentDefinition definition) {
        Map<String, Object> next = new LinkedHashMap<>(document);
        mergeOpenedAt(next, previous);
        if (currentVersion != null
                && !OnboardingStepPayloads.readBoolean(next, OnboardingStepPayloads.CONFIRMED)
                && OnboardingStepPayloads.readNullableString(
                                next, OnboardingStepPayloads.STORAGE_KEY)
                        == null) {
            Integer storedVersion =
                    OnboardingStepPayloads.readInteger(
                            next, OnboardingStepPayloads.DOCUMENT_VERSION);
            if (storedVersion != null && storedVersion != currentVersion) {
                next.remove(OnboardingStepPayloads.OPENED_AT);
            }
        }
        if (OnboardingStepPayloads.readBoolean(next, OnboardingStepPayloads.CONFIRMED)) {
            boolean wasConfirmed =
                    previous != null
                            && OnboardingStepPayloads.readBoolean(
                                    previous, OnboardingStepPayloads.CONFIRMED);
            if (!wasConfirmed) {
                next.put(OnboardingStepPayloads.CONFIRMED_AT, Instant.now().toString());
            } else {
                copyIfPresent(previous, next, OnboardingStepPayloads.CONFIRMED_AT);
            }
        } else {
            next.remove(OnboardingStepPayloads.CONFIRMATION_TEXT);
            next.remove(OnboardingStepPayloads.CONFIRMED_AT);
        }
        if (definition != null) {
            stampOrganizationConfirmationText(next, definition);
            validateDocument(next, definition);
        }
        return next;
    }

    private void stampTenantConfirmationText(
            Map<String, Object> document, TenantOnboardingDocumentDefinition definition) {
        if (!OnboardingStepPayloads.readBoolean(document, OnboardingStepPayloads.CONFIRMED)) {
            return;
        }
        tenantTemplateService
                .findCurrentTemplateMetadata(definition)
                .ifPresent(
                        template ->
                                document.put(
                                        OnboardingStepPayloads.CONFIRMATION_TEXT,
                                        formatConfirmationText(definition.getName(), template)));
    }

    private void stampOrganizationConfirmationText(
            Map<String, Object> document, OrganizationOnboardingDocumentDefinition definition) {
        if (!OnboardingStepPayloads.readBoolean(document, OnboardingStepPayloads.CONFIRMED)) {
            return;
        }
        organizationTemplateService
                .findCurrentTemplateMetadata(definition)
                .ifPresent(
                        template ->
                                document.put(
                                        OnboardingStepPayloads.CONFIRMATION_TEXT,
                                        formatConfirmationText(definition.getName(), template)));
    }

    private String formatConfirmationText(
            String documentName, OnboardingDocumentTemplateService.TemplateMetadata template) {
        String stand =
                template.uploadedAt() != null
                        ? CONFIRMATION_DOCUMENT_DATE.format(template.uploadedAt())
                        : localizationService.getMessage(
                                "onboarding.entryDocument.confirmationDateUnknown", LOCALE);
        return localizationService.getInterpolatedMessage(
                "onboarding.entryDocument.confirmationText",
                Map.of(
                        "documentName", documentName,
                        "version", template.version(),
                        "date", stand),
                LOCALE);
    }

    private void validateDocument(
            Map<String, Object> document, TenantOnboardingDocumentDefinition definition) {
        if (!definition.isMandatory()
                && OnboardingStepPayloads.readBoolean(document, OnboardingStepPayloads.DECLINED)) {
            return;
        }
        String configKey = definition.getConfigKey();
        if (definition.getFulfillmentMode() == OnboardingDocumentFulfillmentMode.CONFIRMED) {
            if (!OnboardingStepPayloads.readBoolean(document, OnboardingStepPayloads.CONFIRMED)) {
                return;
            }
            requireTemplateOpened(
                    configKey, document, tenantTemplateService.hasCurrentTemplate(definition));
            requireConfirmationProof(configKey, document);
            return;
        }
        if (OnboardingStepPayloads.readNullableString(document, OnboardingStepPayloads.STORAGE_KEY)
                != null) {
            requireTemplateOpened(
                    configKey, document, tenantTemplateService.hasCurrentTemplate(definition));
        }
    }

    private void validateDocument(
            Map<String, Object> document, OrganizationOnboardingDocumentDefinition definition) {
        if (!definition.isMandatory()
                && OnboardingStepPayloads.readBoolean(document, OnboardingStepPayloads.DECLINED)) {
            return;
        }
        String configKey = definition.getConfigKey();
        if (definition.getFulfillmentMode() == OnboardingDocumentFulfillmentMode.CONFIRMED) {
            if (!OnboardingStepPayloads.readBoolean(document, OnboardingStepPayloads.CONFIRMED)) {
                return;
            }
            requireTemplateOpened(
                    configKey,
                    document,
                    organizationTemplateService.hasCurrentTemplate(definition));
            requireConfirmationProof(configKey, document);
            return;
        }
        if (OnboardingStepPayloads.readNullableString(document, OnboardingStepPayloads.STORAGE_KEY)
                != null) {
            requireTemplateOpened(
                    configKey,
                    document,
                    organizationTemplateService.hasCurrentTemplate(definition));
        }
    }

    private static void requireTemplateOpened(
            String configKey, Map<String, Object> document, boolean hasTemplate) {
        if (!hasTemplate) {
            return;
        }
        if (OnboardingStepPayloads.readInstant(document, OnboardingStepPayloads.OPENED_AT)
                == null) {
            throw new ConflictException(
                    "Entry document template must be opened before acceptance: " + configKey);
        }
    }

    private static void requireTemplateOpened(
            TenantOnboardingDocumentDefinition definition,
            Map<String, Object> document,
            boolean hasTemplate) {
        requireTemplateOpened(definition.getConfigKey(), document, hasTemplate);
    }

    private static void requireTemplateOpened(
            OrganizationOnboardingDocumentDefinition definition,
            Map<String, Object> document,
            boolean hasTemplate) {
        requireTemplateOpened(definition.getConfigKey(), document, hasTemplate);
    }

    private static void requireConfirmationProof(String configKey, Map<String, Object> document) {
        if (OnboardingStepPayloads.readNullableString(
                        document, OnboardingStepPayloads.CONFIRMATION_TEXT)
                == null) {
            throw new ConflictException(
                    "Entry document confirmation text is required: " + configKey);
        }
        if (OnboardingStepPayloads.readInstant(document, OnboardingStepPayloads.CONFIRMED_AT)
                == null) {
            throw new ConflictException(
                    "Entry document confirmation timestamp is required: " + configKey);
        }
    }

    private void stampVersion(
            Map<String, Object> next, int version, TenantOnboardingDocumentDefinition definition) {
        if (isTenantDocumentFulfilled(next, definition)
                || isTenantDocumentDeclined(next, definition)
                || OnboardingStepPayloads.readBoolean(next, OnboardingStepPayloads.CONFIRMED)
                || OnboardingStepPayloads.readBoolean(next, OnboardingStepPayloads.DECLINED)
                || OnboardingStepPayloads.readNullableString(
                                next, OnboardingStepPayloads.STORAGE_KEY)
                        != null) {
            next.put(OnboardingStepPayloads.DOCUMENT_VERSION, version);
        } else {
            next.remove(OnboardingStepPayloads.DOCUMENT_VERSION);
            next.remove(OnboardingStepPayloads.OPENED_AT);
        }
    }

    private void stampVersion(
            Map<String, Object> next,
            int version,
            OrganizationOnboardingDocumentDefinition definition) {
        if (isOrganizationDocumentFulfilled(next, definition)
                || isOrganizationDocumentDeclined(next, definition)
                || OnboardingStepPayloads.readBoolean(next, OnboardingStepPayloads.CONFIRMED)
                || OnboardingStepPayloads.readBoolean(next, OnboardingStepPayloads.DECLINED)
                || OnboardingStepPayloads.readNullableString(
                                next, OnboardingStepPayloads.STORAGE_KEY)
                        != null) {
            next.put(OnboardingStepPayloads.DOCUMENT_VERSION, version);
        } else {
            next.remove(OnboardingStepPayloads.DOCUMENT_VERSION);
            next.remove(OnboardingStepPayloads.OPENED_AT);
        }
    }

    private static Map<String, Map<String, Object>> documentsByKey(Map<String, Object> payload) {
        Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
        if (payload == null) {
            return byKey;
        }
        for (Map<String, Object> document :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.DOCUMENTS)) {
            String configKey =
                    OnboardingStepPayloads.readString(document, OnboardingStepPayloads.CONFIG_KEY);
            if (!configKey.isBlank()) {
                byKey.put(configKey, document);
            }
        }
        return byKey;
    }

    private static void mergeOpenedAt(Map<String, Object> next, Map<String, Object> previous) {
        if (OnboardingStepPayloads.readInstant(next, OnboardingStepPayloads.OPENED_AT) == null
                && previous != null) {
            copyIfPresent(previous, next, OnboardingStepPayloads.OPENED_AT);
        }
    }

    private static void copyIfPresent(
            Map<String, Object> source, Map<String, Object> target, String key) {
        Object value = source.get(key);
        if (value != null && !(value instanceof String text && text.isBlank())) {
            target.put(key, value);
        }
    }
}
