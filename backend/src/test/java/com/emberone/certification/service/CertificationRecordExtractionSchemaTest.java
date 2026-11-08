/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.emberone.certification.dto.CertificationDefinitionDto;
import com.emberone.platform.ai.extraction.DocumentExtractionResult;
import com.emberone.platform.ai.extraction.ExtractionHint;
import com.emberone.platform.ai.extraction.PreparedExtraction;
import com.emberone.platform.exception.ValidationException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class CertificationRecordExtractionSchemaTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 1);
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Mock CertificationCatalogService catalogService;

    private static CertificationDefinitionDto definition(
            long id, String key, Integer intervalDays) {
        return CertificationDefinitionDto.builder()
                .id(id)
                .configKey(key)
                .name(key + " name")
                .intervalDays(intervalDays)
                .active(true)
                .build();
    }

    private static JsonNode json(String value) {
        return MAPPER.readTree(value);
    }

    @Test
    void acceptsPlausibleDate() {
        DocumentExtractionResult result =
                CertificationRecordExtractionSchema.validate(
                        json("{\"completedDate\":\"2026-05-20\",\"validUntil\":\"2027-05-20\"}"),
                        definition(1, "AS", 365),
                        Set.of("AS"),
                        TODAY);

        assertThat(result.fields()).containsEntry("completedDate", "2026-05-20");
        assertThat(result.hints()).isEmpty();
    }

    @Test
    void dropsFutureDateInsteadOfRepairing() {
        DocumentExtractionResult result =
                CertificationRecordExtractionSchema.validate(
                        json("{\"completedDate\":\"2099-01-01\"}"),
                        definition(1, "AS", 365),
                        Set.of("AS"),
                        TODAY);

        assertThat(result.fields()).doesNotContainKey("completedDate");
        assertThat(result.hints())
                .extracting(ExtractionHint::code)
                .contains(ExtractionHint.FUTURE_DATE, ExtractionHint.NOTHING_RECOGNIZED);
    }

    @Test
    void dropsNonIsoDate() {
        DocumentExtractionResult result =
                CertificationRecordExtractionSchema.validate(
                        json("{\"completedDate\":\"12.03.2025\"}"),
                        definition(1, "AS", 365),
                        Set.of("AS"),
                        TODAY);

        assertThat(result.fields()).isEmpty();
        assertThat(result.hints()).extracting(ExtractionHint::code).contains("VALUE_REJECTED");
    }

    @Test
    void hintsWhenValidityExceedsInterval() {
        DocumentExtractionResult result =
                CertificationRecordExtractionSchema.validate(
                        json("{\"completedDate\":\"2026-05-20\",\"validUntil\":\"2030-05-20\"}"),
                        definition(1, "AS", 365),
                        Set.of("AS"),
                        TODAY);

        assertThat(result.fields()).containsKey("completedDate");
        assertThat(result.hints())
                .extracting(ExtractionHint::code)
                .containsExactly(ExtractionHint.VALIDITY_EXCEEDS_INTERVAL);
    }

    @Test
    void hintsWhenDocumentSeemsToBelongToOtherType() {
        DocumentExtractionResult result =
                CertificationRecordExtractionSchema.validate(
                        json("{\"completedDate\":\"2026-05-20\",\"configKey\":\"FSK\"}"),
                        definition(1, "AS", null),
                        Set.of("AS", "FSK"),
                        TODAY);

        assertThat(result.hints())
                .extracting(ExtractionHint::code)
                .containsExactly(ExtractionHint.TYPE_MISMATCH);
    }

    @Test
    void ignoresInventedConfigKey() {
        DocumentExtractionResult result =
                CertificationRecordExtractionSchema.validate(
                        json("{\"completedDate\":\"2026-05-20\",\"configKey\":\"INVENTED\"}"),
                        definition(1, "AS", null),
                        Set.of("AS"),
                        TODAY);

        assertThat(result.hints()).isEmpty();
    }

    @Test
    void preparesClassBWithCatalog() {
        CertificationRecordExtractionSchema schema =
                new CertificationRecordExtractionSchema(catalogService);
        when(catalogService.listAvailableDefinitionsForUser(5L))
                .thenReturn(List.of(definition(7, "G26", 1095), definition(1, "AS", 365)));

        PreparedExtraction prepared = schema.prepare(5L, 1L);
        assertThat(prepared.instruction()).contains("AS name").contains("G26 name");
        assertThat(prepared.jsonSchema().get("properties").toString()).contains("configKey");
    }

    @Test
    void requiresSelection() {
        CertificationRecordExtractionSchema schema =
                new CertificationRecordExtractionSchema(catalogService);

        assertThatThrownBy(() -> schema.prepare(5L, null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void definitionNotInUsersCatalogIsRejected() {
        CertificationRecordExtractionSchema schema =
                new CertificationRecordExtractionSchema(catalogService);
        when(catalogService.listAvailableDefinitionsForUser(5L))
                .thenReturn(List.of(definition(1, "AS", 365)));

        assertThatThrownBy(() -> schema.prepare(5L, 99L)).isInstanceOf(ValidationException.class);
    }
}
