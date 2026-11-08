/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.drivinglicense.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.drivinglicense.dto.DrivingLicenseDefinitionDto;
import com.emberone.platform.ai.extraction.DocumentExtractionResult;
import com.emberone.platform.ai.extraction.ExtractionHint;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SuppressWarnings("unchecked")
class DrivingLicenseRecordExtractionSchemaTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 1);
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final Map<String, DrivingLicenseDefinitionDto> CATALOG =
            Map.of(
                    "C",
                            DrivingLicenseDefinitionDto.builder()
                                    .id(3L)
                                    .configKey("C")
                                    .name("C")
                                    .active(true)
                                    .build(),
                    "B",
                            DrivingLicenseDefinitionDto.builder()
                                    .id(2L)
                                    .configKey("B")
                                    .name("B")
                                    .active(true)
                                    .build());

    private static JsonNode json(String value) {
        return MAPPER.readTree(value);
    }

    @Test
    void acceptsPlausibleLicense() {
        DocumentExtractionResult result =
                DrivingLicenseRecordExtractionSchema.validate(
                        json(
                                """
                                {"issueDate":"2020-01-10","documentExpirationDate":"2035-01-09",
                                 "licenseNumber":"X123","issuingAuthority":"Landratsamt",
                                 "classes":[{"configKey":"C","grantDate":"2019-02-01","expirationDate":null}]}
                                """),
                        CATALOG,
                        TODAY);

        assertThat(result.fields())
                .containsEntry("issueDate", "2020-01-10")
                .containsEntry("licenseNumber", "X123")
                .containsEntry("issuingAuthority", "Landratsamt");
        List<Map<String, Object>> classes =
                (List<Map<String, Object>>) result.fields().get("classes");
        assertThat(classes).hasSize(1);
        assertThat(classes.getFirst())
                .containsEntry("tenantDrivingLicenseDefinitionId", 3L)
                .containsEntry("grantDate", "2019-02-01");
        assertThat(result.hints()).isEmpty();
    }

    @Test
    void dropsInventedAndDuplicateClasses() {
        DocumentExtractionResult result =
                DrivingLicenseRecordExtractionSchema.validate(
                        json(
                                """
                                {"classes":[{"configKey":"ZZ","grantDate":null,"expirationDate":null},
                                            {"configKey":"B","grantDate":null,"expirationDate":null},
                                            {"configKey":"B","grantDate":null,"expirationDate":null}]}
                                """),
                        CATALOG,
                        TODAY);

        List<Map<String, Object>> classes =
                (List<Map<String, Object>>) result.fields().get("classes");
        assertThat(classes).hasSize(1);
        assertThat(classes.getFirst()).containsEntry("tenantDrivingLicenseDefinitionId", 2L);
        assertThat(result.hints())
                .extracting(ExtractionHint::code)
                .contains(ExtractionHint.VALUE_REJECTED);
    }

    @Test
    void dropsFutureIssueDateAndExpirationBeforeIssue() {
        DocumentExtractionResult future =
                DrivingLicenseRecordExtractionSchema.validate(
                        json("{\"issueDate\":\"2099-01-01\"}"), CATALOG, TODAY);
        assertThat(future.fields()).doesNotContainKey("issueDate");

        DocumentExtractionResult order =
                DrivingLicenseRecordExtractionSchema.validate(
                        json(
                                "{\"issueDate\":\"2020-01-10\",\"documentExpirationDate\":\"2019-01-01\"}"),
                        CATALOG,
                        TODAY);
        assertThat(order.fields())
                .containsKey("issueDate")
                .doesNotContainKey("documentExpirationDate");
        assertThat(order.hints())
                .extracting(ExtractionHint::code)
                .contains(ExtractionHint.DATE_ORDER);
    }

    @Test
    void dropsClassGrantDateAfterCardIssueDate() {
        DocumentExtractionResult result =
                DrivingLicenseRecordExtractionSchema.validate(
                        json(
                                "{\"issueDate\":\"2022-05-07\",\"classes\":[{\"configKey\":\"B\","
                                        + "\"grantDate\":\"2024-01-01\",\"expirationDate\":null}]}"),
                        CATALOG,
                        TODAY);
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> classes =
                (java.util.List<Map<String, Object>>) result.fields().get("classes");
        assertThat(classes).hasSize(1);
        assertThat(classes.getFirst().get("grantDate")).isNull();
        assertThat(result.hints())
                .extracting(ExtractionHint::code)
                .contains(ExtractionHint.DATE_ORDER);
    }

    @Test
    void dropsOverlongTexts() {
        DocumentExtractionResult result =
                DrivingLicenseRecordExtractionSchema.validate(
                        json("{\"licenseNumber\":\"" + "9".repeat(200) + "\"}"), CATALOG, TODAY);

        assertThat(result.fields()).doesNotContainKey("licenseNumber");
    }
}
