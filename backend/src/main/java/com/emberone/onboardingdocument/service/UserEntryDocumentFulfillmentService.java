/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.service;

import com.emberone.onboardingdocument.domain.EntryDocumentFulfillmentOutcome;
import com.emberone.onboardingdocument.domain.EntryDocumentFulfillmentProof;
import com.emberone.onboardingdocument.domain.OnboardingDocumentFulfillmentMode;
import com.emberone.onboardingdocument.domain.UserEntryDocumentFulfillment;
import com.emberone.onboardingdocument.domain.UserEntryDocumentFulfillmentEvent;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinition;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinitionRepository;
import com.emberone.onboardingdocument.repository.UserEntryDocumentFulfillmentEventRepository;
import com.emberone.onboardingdocument.repository.UserEntryDocumentFulfillmentRepository;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinition;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinitionRepository;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.exception.ConflictException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserEntryDocumentFulfillmentService {

    private final UserEntryDocumentFulfillmentRepository fulfillmentRepository;
    private final UserEntryDocumentFulfillmentEventRepository fulfillmentEventRepository;
    private final TenantOnboardingDocumentDefinitionRepository tenantDefinitionRepository;
    private final OrganizationOnboardingDocumentDefinitionRepository orgDefinitionRepository;

    @Transactional(readOnly = true)
    public Set<String> findFulfilledTenantConfigKeys(long userId, Collection<String> configKeys) {
        if (configKeys == null || configKeys.isEmpty()) {
            return Set.of();
        }
        Map<String, TenantOnboardingDocumentDefinition> definitions =
                loadCurrentTenantDefinitions(configKeys);
        Set<String> fulfilled = new HashSet<>();
        for (UserEntryDocumentFulfillment fulfillment :
                fulfillmentRepository.findByUserIdAndConfigKeyInAndOrganizationIdIsNull(
                        userId, configKeys)) {
            TenantOnboardingDocumentDefinition definition =
                    definitions.get(fulfillment.getConfigKey());
            if (definition != null
                    && fulfillment.isAcceptedForCurrentTemplate(definition.getVersion())) {
                fulfilled.add(fulfillment.getConfigKey());
            }
        }
        return fulfilled;
    }

    @Transactional(readOnly = true)
    public Set<String> findDeclinedTenantConfigKeys(long userId, Collection<String> configKeys) {
        if (configKeys == null || configKeys.isEmpty()) {
            return Set.of();
        }
        Map<String, TenantOnboardingDocumentDefinition> definitions =
                loadCurrentTenantDefinitions(configKeys);
        Set<String> declined = new HashSet<>();
        for (UserEntryDocumentFulfillment fulfillment :
                fulfillmentRepository.findByUserIdAndConfigKeyInAndOrganizationIdIsNull(
                        userId, configKeys)) {
            TenantOnboardingDocumentDefinition definition =
                    definitions.get(fulfillment.getConfigKey());
            if (definition != null
                    && fulfillment.isDeclinedForCurrentTemplate(definition.getVersion())) {
                declined.add(fulfillment.getConfigKey());
            }
        }
        return declined;
    }

    @Transactional(readOnly = true)
    public Set<String> findFulfilledOrganizationConfigKeys(
            long organizationId, long userId, Collection<String> configKeys) {
        if (configKeys == null || configKeys.isEmpty()) {
            return Set.of();
        }
        Map<String, OrganizationOnboardingDocumentDefinition> definitions =
                loadCurrentOrganizationDefinitions(organizationId, configKeys);
        Set<String> fulfilled = new HashSet<>();
        for (UserEntryDocumentFulfillment fulfillment :
                fulfillmentRepository.findByOrganizationIdAndUserIdAndConfigKeyIn(
                        organizationId, userId, configKeys)) {
            OrganizationOnboardingDocumentDefinition definition =
                    definitions.get(fulfillment.getConfigKey());
            if (definition != null
                    && fulfillment.isAcceptedForCurrentTemplate(definition.getVersion())) {
                fulfilled.add(fulfillment.getConfigKey());
            }
        }
        return fulfilled;
    }

    @Transactional(readOnly = true)
    public Set<String> findDeclinedOrganizationConfigKeys(
            long organizationId, long userId, Collection<String> configKeys) {
        if (configKeys == null || configKeys.isEmpty()) {
            return Set.of();
        }
        Map<String, OrganizationOnboardingDocumentDefinition> definitions =
                loadCurrentOrganizationDefinitions(organizationId, configKeys);
        Set<String> declined = new HashSet<>();
        for (UserEntryDocumentFulfillment fulfillment :
                fulfillmentRepository.findByOrganizationIdAndUserIdAndConfigKeyIn(
                        organizationId, userId, configKeys)) {
            OrganizationOnboardingDocumentDefinition definition =
                    definitions.get(fulfillment.getConfigKey());
            if (definition != null
                    && fulfillment.isDeclinedForCurrentTemplate(definition.getVersion())) {
                declined.add(fulfillment.getConfigKey());
            }
        }
        return declined;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> findTenantPromotedUserDocumentIds(
            long userId, Collection<String> configKeys) {
        if (configKeys == null || configKeys.isEmpty()) {
            return Map.of();
        }
        return fulfillmentRepository
                .findByUserIdAndConfigKeyInAndOrganizationIdIsNull(userId, configKeys)
                .stream()
                .filter(fulfillment -> fulfillment.getUserDocumentId() != null)
                .filter(
                        fulfillment ->
                                fulfillment.getOutcome()
                                        == EntryDocumentFulfillmentOutcome.ACCEPTED)
                .collect(
                        Collectors.toMap(
                                UserEntryDocumentFulfillment::getConfigKey,
                                UserEntryDocumentFulfillment::getUserDocumentId,
                                (left, right) -> left,
                                LinkedHashMap::new));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> findOrganizationPromotedUserDocumentIds(
            long organizationId, long userId, Collection<String> configKeys) {
        if (configKeys == null || configKeys.isEmpty()) {
            return Map.of();
        }
        return fulfillmentRepository
                .findByOrganizationIdAndUserIdAndConfigKeyIn(organizationId, userId, configKeys)
                .stream()
                .filter(fulfillment -> fulfillment.getUserDocumentId() != null)
                .filter(
                        fulfillment ->
                                fulfillment.getOutcome()
                                        == EntryDocumentFulfillmentOutcome.ACCEPTED)
                .collect(
                        Collectors.toMap(
                                UserEntryDocumentFulfillment::getConfigKey,
                                UserEntryDocumentFulfillment::getUserDocumentId,
                                (left, right) -> left,
                                LinkedHashMap::new));
    }

    @Transactional(readOnly = true)
    public boolean areAllTenantFulfilled(long userId, Collection<String> configKeys) {
        if (configKeys == null || configKeys.isEmpty()) {
            return false;
        }
        Map<String, TenantOnboardingDocumentDefinition> definitions =
                loadCurrentTenantDefinitions(configKeys);
        List<String> mandatoryKeys =
                configKeys.stream()
                        .filter(
                                key -> {
                                    TenantOnboardingDocumentDefinition definition =
                                            definitions.get(key);
                                    return definition != null && definition.isMandatory();
                                })
                        .toList();
        if (mandatoryKeys.isEmpty()) {
            return true;
        }
        Set<String> fulfilled = findFulfilledTenantConfigKeys(userId, mandatoryKeys);
        return fulfilled.size() == mandatoryKeys.size();
    }

    @Transactional(readOnly = true)
    public boolean areAllOrganizationFulfilled(
            long organizationId, long userId, Collection<String> configKeys) {
        if (configKeys == null || configKeys.isEmpty()) {
            return true;
        }
        Map<String, OrganizationOnboardingDocumentDefinition> definitions =
                loadCurrentOrganizationDefinitions(organizationId, configKeys);
        List<String> mandatoryKeys =
                configKeys.stream()
                        .filter(
                                key -> {
                                    OrganizationOnboardingDocumentDefinition definition =
                                            definitions.get(key);
                                    return definition != null && definition.isMandatory();
                                })
                        .toList();
        if (mandatoryKeys.isEmpty()) {
            return true;
        }
        Set<String> fulfilled =
                findFulfilledOrganizationConfigKeys(organizationId, userId, mandatoryKeys);
        return fulfilled.size() == mandatoryKeys.size();
    }

    @Transactional
    public UserEntryDocumentFulfillment upsertTenant(
            long tenantId,
            long userId,
            String configKey,
            OnboardingDocumentFulfillmentMode mode,
            Long userDocumentId,
            EntryDocumentFulfillmentProof proof) {
        return upsert(tenantId, null, userId, configKey, mode, userDocumentId, proof);
    }

    @Transactional
    public UserEntryDocumentFulfillment upsertOrganization(
            long tenantId,
            long organizationId,
            long userId,
            String configKey,
            OnboardingDocumentFulfillmentMode mode,
            Long userDocumentId,
            EntryDocumentFulfillmentProof proof) {
        return upsert(tenantId, organizationId, userId, configKey, mode, userDocumentId, proof);
    }

    @Transactional
    public UserEntryDocumentFulfillment recordDecline(
            long tenantId,
            Long organizationId,
            long userId,
            String configKey,
            EntryDocumentFulfillmentProof proof) {
        if (proof.outcome() != EntryDocumentFulfillmentOutcome.DECLINED) {
            throw new IllegalArgumentException("Decline proof must use outcome DECLINED");
        }
        OnboardingDocumentFulfillmentMode mode = resolveFulfillmentMode(organizationId, configKey);
        return upsert(tenantId, organizationId, userId, configKey, mode, null, proof);
    }

    private UserEntryDocumentFulfillment upsert(
            long tenantId,
            Long organizationId,
            long userId,
            String configKey,
            OnboardingDocumentFulfillmentMode mode,
            Long userDocumentId,
            EntryDocumentFulfillmentProof proof) {
        Instant confirmedAt = proof.confirmedAt() != null ? proof.confirmedAt() : Instant.now();
        UserEntryDocumentFulfillment fulfillment =
                findExisting(organizationId, userId, configKey)
                        .orElseGet(
                                () ->
                                        UserEntryDocumentFulfillment.builder()
                                                .tenantId(tenantId)
                                                .organizationId(organizationId)
                                                .userId(userId)
                                                .configKey(configKey)
                                                .build());
        fulfillment.setTemplateVersion(proof.templateVersion());
        fulfillment.setTemplateSha256Hex(proof.templateSha256Hex());
        fulfillment.setOutcome(proof.outcome());
        if (mode != null) {
            fulfillment.setMode(mode);
        }
        if (userDocumentId != null) {
            fulfillment.setUserDocumentId(userDocumentId);
        }
        if (proof.outcome() == EntryDocumentFulfillmentOutcome.ACCEPTED) {
            fulfillment.setConfirmedAt(confirmedAt);
        } else {
            fulfillment.setConfirmedAt(null);
            fulfillment.setUserDocumentId(null);
        }
        UserEntryDocumentFulfillment saved = fulfillmentRepository.save(fulfillment);
        appendEvent(
                tenantId,
                organizationId,
                userId,
                configKey,
                saved.getMode(),
                saved.getUserDocumentId(),
                proof,
                confirmedAt);
        return saved;
    }

    private void appendEvent(
            long tenantId,
            Long organizationId,
            long userId,
            String configKey,
            OnboardingDocumentFulfillmentMode mode,
            Long userDocumentId,
            EntryDocumentFulfillmentProof proof,
            Instant confirmedAt) {
        fulfillmentEventRepository.save(
                UserEntryDocumentFulfillmentEvent.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .organizationId(organizationId)
                        .configKey(configKey)
                        .templateVersion(proof.templateVersion())
                        .templateSha256Hex(proof.templateSha256Hex())
                        .confirmationText(proof.confirmationText())
                        .outcome(proof.outcome())
                        .channel(proof.channel())
                        .confirmedAt(confirmedAt)
                        .actorUserId(proof.actorUserId())
                        .adminRecordReason(proof.adminRecordReason())
                        .workflowReviewerUserId(proof.workflowReviewerUserId())
                        .workflowReviewedAt(proof.workflowReviewedAt())
                        .userDocumentId(userDocumentId)
                        .build());
    }

    private java.util.Optional<UserEntryDocumentFulfillment> findExisting(
            Long organizationId, long userId, String configKey) {
        if (organizationId == null) {
            return fulfillmentRepository.findByUserIdAndConfigKeyAndOrganizationIdIsNull(
                    userId, configKey);
        }
        return fulfillmentRepository.findByOrganizationIdAndUserIdAndConfigKey(
                organizationId, userId, configKey);
    }

    private Map<String, TenantOnboardingDocumentDefinition> loadCurrentTenantDefinitions(
            Collection<String> configKeys) {
        Map<String, TenantOnboardingDocumentDefinition> definitions = new LinkedHashMap<>();
        for (String configKey : configKeys) {
            tenantDefinitionRepository
                    .findByConfigKey(configKey)
                    .filter(LifecycleFields::isCatalogSelectable)
                    .ifPresent(definition -> definitions.put(configKey, definition));
        }
        return definitions;
    }

    private OnboardingDocumentFulfillmentMode resolveFulfillmentMode(
            Long organizationId, String configKey) {
        if (organizationId == null) {
            return tenantDefinitionRepository
                    .findByConfigKey(configKey)
                    .map(TenantOnboardingDocumentDefinition::getFulfillmentMode)
                    .orElseThrow(
                            () ->
                                    new ConflictException(
                                            "Unknown tenant onboarding document: " + configKey));
        }
        return orgDefinitionRepository
                .findByOrganizationIdAndConfigKey(organizationId, configKey)
                .map(OrganizationOnboardingDocumentDefinition::getFulfillmentMode)
                .orElseThrow(
                        () ->
                                new ConflictException(
                                        "Unknown organization onboarding document: " + configKey));
    }

    private Map<String, OrganizationOnboardingDocumentDefinition>
            loadCurrentOrganizationDefinitions(long organizationId, Collection<String> configKeys) {
        Map<String, OrganizationOnboardingDocumentDefinition> definitions = new LinkedHashMap<>();
        for (OrganizationOnboardingDocumentDefinition definition :
                orgDefinitionRepository.findByOrganizationIdAndConfigKeyInAndArchivedFalse(
                        organizationId, configKeys)) {
            if (LifecycleFields.isCatalogSelectable(definition)) {
                definitions.put(definition.getConfigKey(), definition);
            }
        }
        return definitions;
    }
}
