/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.certification.service;

import com.emberone.certification.dto.CertificationDefinitionDto;
import com.emberone.platform.ai.extraction.DocumentExtractionResult;
import com.emberone.platform.ai.extraction.DocumentExtractionSchema;
import com.emberone.platform.ai.extraction.ExtractionHint;
import com.emberone.platform.ai.extraction.ExtractionJson;
import com.emberone.platform.ai.extraction.PreparedExtraction;
import com.emberone.platform.exception.ValidationException;
import com.emberone.platform.time.OrganizationWallClockTime;
import com.emberone.workflow.domain.WorkflowType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Extraction schema for {@link WorkflowType#CERTIFICATION_RECORD}: suggests the completion date for
 * the definition the user already selected (data class {@code B}).
 */
@Component
@RequiredArgsConstructor
public class CertificationRecordExtractionSchema implements DocumentExtractionSchema {

    private final CertificationCatalogService catalogService;

    @Override
    public String type() {
        return WorkflowType.CERTIFICATION_RECORD.name();
    }

    @Override
    public PreparedExtraction prepare(long userId, Long selectionId) {
        if (selectionId == null) {
            throw new ValidationException("A certification definition must be selected first");
        }
        catalogService.requireTypeAvailableForUser(selectionId, userId);
        List<CertificationDefinitionDto> available =
                catalogService.listAvailableDefinitionsForUser(userId);
        CertificationDefinitionDto selected =
                available.stream()
                        .filter(definition -> Objects.equals(definition.getId(), selectionId))
                        .findFirst()
                        .orElseThrow(() ->
                                new ValidationException(
                                        "Certification definition is not available"));

        Map<String, String> allowedKeys = new LinkedHashMap<>();
        for (CertificationDefinitionDto definition : available) {
            if (definition.getConfigKey() != null) {
                allowedKeys.put(definition.getConfigKey(), definition.getName());
            }
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(
                "completedDate",
                ExtractionJson.nullableDate(
                        "Date the training/examination was completed or the certificate was"
                                + " issued."));
        properties.put(
                "validUntil",
                ExtractionJson.nullableDate(
                        "Date until which the certificate is valid, if stated."));
        if (!allowedKeys.isEmpty()) {
            properties.put(
                    "configKey",
                    ExtractionJson.nullableEnum(
                            allowedKeys.keySet(),
                            "Which catalog entry the document belongs to, or null."));
        }

        String instruction =
                "The document is a certification proof. Catalog entries:\n"
                        + ExtractionJson.catalogList(allowedKeys);

        LocalDate today = OrganizationWallClockTime.today();
        Set<String> keys = Set.copyOf(allowedKeys.keySet());
        return new PreparedExtraction(
                "certification_record_extraction",
                instruction,
                ExtractionJson.object(properties),
                answer -> validate(answer, selected, keys, today));
    }

    /** Deterministic plausibility rules; invalid values are dropped, never repaired. */
    static DocumentExtractionResult validate(
            JsonNode answer,
            CertificationDefinitionDto selected,
            Set<String> allowedKeys,
            LocalDate today) {
        Map<String, Object> fields = new LinkedHashMap<>();
        List<ExtractionHint> hints = new ArrayList<>();

        Optional<LocalDate> completed = ExtractionJson.date(answer.get("completedDate"));
        if (completed.isPresent() && completed.get().isAfter(today)) {
            hints.add(new ExtractionHint(ExtractionHint.FUTURE_DATE, "completedDate"));
            completed = Optional.empty();
        } else if (completed.isEmpty() && isPresent(answer.get("completedDate"))) {
            hints.add(new ExtractionHint(ExtractionHint.VALUE_REJECTED, "completedDate"));
        }
        completed.ifPresent(date -> fields.put("completedDate", date.toString()));

        Optional<LocalDate> validUntil = ExtractionJson.date(answer.get("validUntil"));
        if (completed.isPresent() && validUntil.isPresent()) {
            if (validUntil.get().isBefore(completed.get())) {
                hints.add(new ExtractionHint(ExtractionHint.DATE_ORDER, "completedDate"));
            } else if (selected.getIntervalDays() != null
                    && validUntil
                            .get()
                            .isAfter(completed.get().plusDays(selected.getIntervalDays()))) {
                hints.add(
                        new ExtractionHint(
                                ExtractionHint.VALIDITY_EXCEEDS_INTERVAL, "completedDate"));
            }
        }

        JsonNode keyNode = answer.get("configKey");
        if (keyNode != null && keyNode.isString()) {
            String key = keyNode.asString();
            if (allowedKeys.contains(key) && !key.equals(selected.getConfigKey())) {
                hints.add(
                        new ExtractionHint(ExtractionHint.TYPE_MISMATCH, "tenantCertDefinitionId"));
            }
        }

        if (fields.isEmpty()) {
            hints.add(new ExtractionHint(ExtractionHint.NOTHING_RECOGNIZED, null));
        }
        return new DocumentExtractionResult(fields, hints);
    }

    private static boolean isPresent(JsonNode node) {
        return node != null && !node.isNull() && !(node.isString() && node.asString().isBlank());
    }
}
