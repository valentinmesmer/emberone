/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.service;

import com.emberone.onboarding.config.EntryDocumentsStepConfig;
import com.emberone.onboarding.config.OnboardingStepConfig;
import com.emberone.onboarding.domain.OnboardingStepType;
import com.emberone.onboarding.handler.EntryDocumentsStepHandler;
import com.emberone.onboarding.spi.OnboardingStepHandler;
import com.emberone.onboarding.spi.OnboardingStepHandlerRegistry;
import com.emberone.onboarding.standard.StandardOnboardingStep;
import com.emberone.onboarding.standard.StandardOnboardingStepRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.standard.domain.Standard;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StandardOnboardingStepService {

    private final StandardOnboardingStepRepository stepRepository;
    private final OnboardingStepHandlerRegistry handlerRegistry;

    @Transactional(readOnly = true)
    public List<StandardOnboardingStep> listSteps(Standard standard) {
        return stepRepository.findByStandardIdOrderBySortOrderAscIdAsc(standard.getId());
    }

    @Transactional
    public List<StandardOnboardingStep> replaceSteps(
            Standard standard, List<StepReplacement> replacements) {
        validateUniqueTypes(replacements);
        long tenantId = standard.getTenantId();
        Long standardId = standard.getId();

        Map<OnboardingStepType, StandardOnboardingStep> existingByType = new LinkedHashMap<>();
        for (StandardOnboardingStep step :
                stepRepository.findByStandardIdOrderBySortOrderAscIdAsc(standardId)) {
            existingByType.put(step.getStepType(), step);
        }

        Set<OnboardingStepType> requestedTypes = new HashSet<>();
        List<StandardOnboardingStep> saved = new ArrayList<>();
        int sortOrder = 0;
        for (StepReplacement replacement : replacements) {
            OnboardingStepType stepType = replacement.stepType();
            if (!requestedTypes.add(stepType)) {
                throw new ConflictException("Duplicate onboarding step type: " + stepType);
            }
            OnboardingStepHandler handler = handlerRegistry.require(stepType);
            OnboardingStepConfig config = handler.parseConfig(replacement.config());
            handler.validateConfig(tenantId, config);
            Map<String, Object> configMap = new HashMap<>(handler.toConfigMap(config));

            StandardOnboardingStep step = existingByType.get(stepType);
            if (step == null) {
                step =
                        StandardOnboardingStep.builder()
                                .tenantId(tenantId)
                                .standardId(standardId)
                                .stepType(stepType)
                                .build();
            }
            step.setSortOrder(sortOrder++);
            step.setConfig(configMap);
            saved.add(stepRepository.save(step));
        }

        for (StandardOnboardingStep existing : existingByType.values()) {
            if (!requestedTypes.contains(existing.getStepType())) {
                stepRepository.delete(existing);
            }
        }
        return saved;
    }

    @Transactional
    public void copySteps(Standard source, Standard target) {
        List<StandardOnboardingStep> sourceSteps =
                stepRepository.findByStandardIdOrderBySortOrderAscIdAsc(source.getId());
        stepRepository.deleteByStandardId(target.getId());
        for (StandardOnboardingStep sourceStep : sourceSteps) {
            stepRepository.save(
                    StandardOnboardingStep.builder()
                            .tenantId(target.getTenantId())
                            .standardId(target.getId())
                            .stepType(sourceStep.getStepType())
                            .sortOrder(sourceStep.getSortOrder())
                            .config(new HashMap<>(sourceStep.getConfig()))
                            .build());
        }
    }

    @Transactional
    public void ensureSystemOnboardingSteps(
            Standard standard, List<OnboardingStepType> requiredStepTypes) {
        if (requiredStepTypes == null || requiredStepTypes.isEmpty()) {
            return;
        }
        List<StandardOnboardingStep> existing =
                stepRepository.findByStandardIdOrderBySortOrderAscIdAsc(standard.getId());
        Set<OnboardingStepType> present =
                existing.stream()
                        .map(StandardOnboardingStep::getStepType)
                        .collect(java.util.stream.Collectors.toSet());
        List<StepReplacement> replacements = new ArrayList<>();
        int sortOrder = 0;
        for (StandardOnboardingStep step : existing) {
            replacements.add(
                    new StepReplacement(
                            step.getStepType(), sortOrder++, new HashMap<>(step.getConfig())));
        }
        for (OnboardingStepType stepType : requiredStepTypes) {
            if (present.contains(stepType)) {
                continue;
            }
            replacements.add(new StepReplacement(stepType, sortOrder++, Map.of()));
        }
        replaceSteps(standard, replacements);
    }

    @Transactional
    public void replaceEntryDocumentsStep(Standard standard, List<String> documentConfigKeys) {
        List<StepReplacement> replacements = new ArrayList<>();
        for (StandardOnboardingStep step :
                stepRepository.findByStandardIdOrderBySortOrderAscIdAsc(standard.getId())) {
            if (step.getStepType() == OnboardingStepType.ENTRY_DOCUMENTS
                    && documentConfigKeys.isEmpty()) {
                continue;
            }
            Map<String, Object> config =
                    step.getStepType() == OnboardingStepType.ENTRY_DOCUMENTS
                            ? Map.of(
                                    EntryDocumentsStepHandler.DOCUMENT_CONFIG_KEYS,
                                    documentConfigKeys)
                            : new HashMap<>(step.getConfig());
            replacements.add(new StepReplacement(step.getStepType(), step.getSortOrder(), config));
        }
        if (replacements.stream().noneMatch(r -> r.stepType() == OnboardingStepType.ENTRY_DOCUMENTS)
                && !documentConfigKeys.isEmpty()) {
            replacements.add(
                    new StepReplacement(
                            OnboardingStepType.ENTRY_DOCUMENTS,
                            replacements.size(),
                            Map.of(
                                    EntryDocumentsStepHandler.DOCUMENT_CONFIG_KEYS,
                                    documentConfigKeys)));
        }
        replacements.sort((a, b) -> Integer.compare(a.sortOrder(), b.sortOrder()));
        List<StepReplacement> normalized = new ArrayList<>();
        int order = 0;
        for (StepReplacement replacement : replacements) {
            normalized.add(
                    new StepReplacement(replacement.stepType(), order++, replacement.config()));
        }
        replaceSteps(standard, normalized);
    }

    @Transactional
    public void removeDocumentConfigKey(String configKey) {
        for (StandardOnboardingStep step :
                stepRepository.findByStepType(OnboardingStepType.ENTRY_DOCUMENTS)) {
            OnboardingStepHandler handler =
                    handlerRegistry.require(OnboardingStepType.ENTRY_DOCUMENTS);
            OnboardingStepConfig parsed = handler.parseConfig(step.getConfig());
            if (!(parsed instanceof EntryDocumentsStepConfig entryConfig)) {
                continue;
            }
            List<String> remaining =
                    entryConfig.documentConfigKeys().stream()
                            .filter(key -> !key.equals(configKey))
                            .toList();
            if (remaining.size() == entryConfig.documentConfigKeys().size()) {
                continue;
            }
            if (remaining.isEmpty()) {
                stepRepository.delete(step);
            } else {
                step.setConfig(
                        new HashMap<>(
                                handler.toConfigMap(new EntryDocumentsStepConfig(remaining))));
                stepRepository.save(step);
            }
        }
    }

    @Transactional
    public void renameDocumentConfigKey(String oldKey, String newKey) {
        if (oldKey.equals(newKey)) {
            return;
        }
        for (StandardOnboardingStep step :
                stepRepository.findByStepType(OnboardingStepType.ENTRY_DOCUMENTS)) {
            OnboardingStepHandler handler =
                    handlerRegistry.require(OnboardingStepType.ENTRY_DOCUMENTS);
            OnboardingStepConfig parsed = handler.parseConfig(step.getConfig());
            if (!(parsed instanceof EntryDocumentsStepConfig entryConfig)) {
                continue;
            }
            List<String> next =
                    entryConfig.documentConfigKeys().stream()
                            .map(key -> key.equals(oldKey) ? newKey : key)
                            .toList();
            if (next.equals(entryConfig.documentConfigKeys())) {
                continue;
            }
            step.setConfig(new HashMap<>(handler.toConfigMap(new EntryDocumentsStepConfig(next))));
            stepRepository.save(step);
        }
    }

    private static void validateUniqueTypes(List<StepReplacement> replacements) {
        Set<OnboardingStepType> seen = new HashSet<>();
        for (StepReplacement replacement : replacements) {
            if (!seen.add(replacement.stepType())) {
                throw new ConflictException(
                        "Duplicate onboarding step type: " + replacement.stepType());
            }
        }
    }

    public record StepReplacement(
            OnboardingStepType stepType, int sortOrder, Map<String, Object> config) {}
}
