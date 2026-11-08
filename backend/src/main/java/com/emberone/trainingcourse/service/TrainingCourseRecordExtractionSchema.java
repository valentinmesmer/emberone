/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.trainingcourse.service;

import com.emberone.platform.ai.extraction.DocumentExtractionResult;
import com.emberone.platform.ai.extraction.DocumentExtractionSchema;
import com.emberone.platform.ai.extraction.ExtractionHint;
import com.emberone.platform.ai.extraction.ExtractionJson;
import com.emberone.platform.ai.extraction.PreparedExtraction;
import com.emberone.platform.time.OrganizationWallClockTime;
import com.emberone.trainingcourse.dto.TrainingCourseTypeDto;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.TrainingCourseRecordWorkflowPayload;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Extraction schema for {@link WorkflowType#TRAINING_COURSE_RECORD}: course types (from the user's
 * catalog), dates, location, hours and name.
 */
@Component
@RequiredArgsConstructor
public class TrainingCourseRecordExtractionSchema implements DocumentExtractionSchema {

    private static final BigDecimal MAX_HOURS = new BigDecimal("1000");

    private final TrainingCourseCatalogService catalogService;

    @Override
    public String type() {
        return WorkflowType.TRAINING_COURSE_RECORD.name();
    }

    @Override
    public PreparedExtraction prepare(long userId, Long selectionId) {
        Map<String, TrainingCourseTypeDto> byKey = new LinkedHashMap<>();
        for (TrainingCourseTypeDto type : catalogService.listAvailableTypesForUser(userId)) {
            if (type.isActive() && type.getConfigKey() != null) {
                byKey.put(type.getConfigKey(), type);
            }
        }
        Map<String, String> keyToName = new LinkedHashMap<>();
        byKey.forEach((key, type) -> keyToName.put(key, type.getName()));

        Map<String, Object> properties = new LinkedHashMap<>();
        if (!byKey.isEmpty()) {
            properties.put(
                    "typeConfigKeys",
                    ExtractionJson.arrayOf(
                            ExtractionJson.enumString(byKey.keySet()),
                            "Catalog entries the course covers (usually one). Empty if none fits."));
        }
        properties.put("name", ExtractionJson.nullableString("Title of the course, if stated."));
        properties.put("startDate", ExtractionJson.nullableDate("First day of the course."));
        properties.put("endDate", ExtractionJson.nullableDate("Last day of the course."));
        properties.put(
                "location", ExtractionJson.nullableString("Place where the course took place."));
        properties.put("hours", ExtractionJson.nullableNumber("Duration in hours, if stated."));

        LocalDate today = OrganizationWallClockTime.today();
        return new PreparedExtraction(
                "training_course_record_extraction",
                "The document is a training course proof. Catalog entries:\n"
                        + ExtractionJson.catalogList(keyToName),
                ExtractionJson.object(properties),
                answer -> validate(answer, byKey, today));
    }

    /** Deterministic plausibility rules; invalid values are dropped, never repaired. */
    static DocumentExtractionResult validate(
            JsonNode answer, Map<String, TrainingCourseTypeDto> catalogByKey, LocalDate today) {
        Map<String, Object> fields = new LinkedHashMap<>();
        List<ExtractionHint> hints = new ArrayList<>();

        JsonNode keys = answer.get("typeConfigKeys");
        if (keys != null && keys.isArray()) {
            Set<Long> ids = new LinkedHashSet<>();
            boolean rejected = false;
            for (JsonNode keyNode : keys) {
                TrainingCourseTypeDto type =
                        keyNode.isString() ? catalogByKey.get(keyNode.asString()) : null;
                if (type == null) {
                    rejected = true;
                } else {
                    ids.add(type.getId());
                }
            }
            if (ids.size() > TrainingCourseRecordWorkflowPayload.MAX_GRANTED_TYPES) {
                ids.clear();
                rejected = true;
            }
            if (!ids.isEmpty()) {
                fields.put("tenantTrainingCourseTypeIds", List.copyOf(ids));
            }
            if (rejected) {
                hints.add(
                        new ExtractionHint(
                                ExtractionHint.VALUE_REJECTED, "tenantTrainingCourseTypeIds"));
            }
        }

        Optional<LocalDate> start = ExtractionJson.date(answer.get("startDate"));
        Optional<LocalDate> end = ExtractionJson.date(answer.get("endDate"));
        if (start.isPresent() && start.get().isAfter(today)) {
            hints.add(new ExtractionHint(ExtractionHint.FUTURE_DATE, "startDate"));
            start = Optional.empty();
        }
        if (end.isPresent() && end.get().isAfter(today)) {
            hints.add(new ExtractionHint(ExtractionHint.FUTURE_DATE, "endDate"));
            end = Optional.empty();
        }
        if (start.isPresent() && end.isPresent() && end.get().isBefore(start.get())) {
            hints.add(new ExtractionHint(ExtractionHint.DATE_ORDER, "endDate"));
            start = Optional.empty();
            end = Optional.empty();
        }
        start.ifPresent(date -> fields.put("startDate", date.toString()));
        end.ifPresent(date -> fields.put("endDate", date.toString()));

        ExtractionJson.text(answer.get("location"), 512)
                .ifPresent(value -> fields.put("location", value));
        ExtractionJson.text(answer.get("name"), TrainingCourseRecordWorkflowPayload.MAX_NAME_LENGTH)
                .ifPresent(value -> fields.put("name", value));

        Optional<BigDecimal> hours = ExtractionJson.number(answer.get("hours"));
        if (hours.isPresent()) {
            BigDecimal value = hours.get();
            if (value.signum() > 0 && value.compareTo(MAX_HOURS) <= 0 && value.scale() <= 2) {
                fields.put("hours", value);
            } else {
                hints.add(new ExtractionHint(ExtractionHint.VALUE_REJECTED, "hours"));
            }
        }

        if (fields.isEmpty()) {
            hints.add(new ExtractionHint(ExtractionHint.NOTHING_RECOGNIZED, null));
        }
        return new DocumentExtractionResult(fields, hints);
    }
}
