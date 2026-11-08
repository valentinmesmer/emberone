/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.trainingcourse.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.platform.ai.extraction.DocumentExtractionResult;
import com.emberone.platform.ai.extraction.ExtractionHint;
import com.emberone.trainingcourse.dto.TrainingCourseTypeDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class TrainingCourseRecordExtractionSchemaTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 1);
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final Map<String, TrainingCourseTypeDto> CATALOG =
            Map.of(
                    "TC_MA",
                    TrainingCourseTypeDto.builder()
                            .id(11L)
                            .configKey("TC_MA")
                            .name("Maschinist")
                            .active(true)
                            .build());

    private static JsonNode json(String value) {
        return MAPPER.readTree(value);
    }

    @Test
    void mapsCatalogKeysToIdsAndKeepsPlausibleValues() {
        DocumentExtractionResult result =
                TrainingCourseRecordExtractionSchema.validate(
                        json(
                                """
                                {"typeConfigKeys":["TC_MA"],"name":"Maschinistenlehrgang",
                                 "startDate":"2026-03-02","endDate":"2026-03-06",
                                 "location":"Feuerwehrschule","hours":35.5}
                                """),
                        CATALOG,
                        TODAY);

        assertThat(result.fields())
                .containsEntry("tenantTrainingCourseTypeIds", List.of(11L))
                .containsEntry("startDate", "2026-03-02")
                .containsEntry("endDate", "2026-03-06")
                .containsEntry("location", "Feuerwehrschule")
                .containsEntry("hours", new BigDecimal("35.5"));
        assertThat(result.hints()).isEmpty();
    }

    @Test
    void discardsUnknownTypeKeysAndHints() {
        DocumentExtractionResult result =
                TrainingCourseRecordExtractionSchema.validate(
                        json("{\"typeConfigKeys\":[\"INVENTED\"],\"location\":\"X\"}"),
                        CATALOG,
                        TODAY);

        assertThat(result.fields()).doesNotContainKey("tenantTrainingCourseTypeIds");
        assertThat(result.hints())
                .extracting(ExtractionHint::field)
                .contains("tenantTrainingCourseTypeIds");
    }

    @Test
    void dropsBothDatesWhenOrderIsInvalid() {
        DocumentExtractionResult result =
                TrainingCourseRecordExtractionSchema.validate(
                        json("{\"startDate\":\"2026-03-06\",\"endDate\":\"2026-03-02\"}"),
                        CATALOG,
                        TODAY);

        assertThat(result.fields()).doesNotContainKeys("startDate", "endDate");
        assertThat(result.hints())
                .extracting(ExtractionHint::code)
                .contains(ExtractionHint.DATE_ORDER);
    }

    @Test
    void dropsFutureEndDate() {
        DocumentExtractionResult result =
                TrainingCourseRecordExtractionSchema.validate(
                        json("{\"startDate\":\"2026-03-02\",\"endDate\":\"2099-03-06\"}"),
                        CATALOG,
                        TODAY);

        assertThat(result.fields()).containsKey("startDate").doesNotContainKey("endDate");
        assertThat(result.hints())
                .extracting(ExtractionHint::code)
                .contains(ExtractionHint.FUTURE_DATE);
    }

    @Test
    void rejectsImplausibleHours() {
        for (String hours : new String[] {"-3", "0", "5000", "1.234"}) {
            DocumentExtractionResult result =
                    TrainingCourseRecordExtractionSchema.validate(
                            json("{\"hours\":" + hours + ",\"location\":\"X\"}"), CATALOG, TODAY);

            assertThat(result.fields()).doesNotContainKey("hours");
        }
    }

    @Test
    void emptyAnswerYieldsNothingRecognizedHint() {
        DocumentExtractionResult result =
                TrainingCourseRecordExtractionSchema.validate(json("{}"), CATALOG, TODAY);

        assertThat(result.fields()).isEmpty();
        assertThat(result.hints())
                .extracting(ExtractionHint::code)
                .containsExactly(ExtractionHint.NOTHING_RECOGNIZED);
    }
}
