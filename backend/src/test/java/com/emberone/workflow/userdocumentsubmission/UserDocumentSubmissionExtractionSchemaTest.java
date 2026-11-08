/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.userdocumentsubmission;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.platform.ai.extraction.DocumentExtractionResult;
import com.emberone.platform.ai.extraction.ExtractionHint;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class UserDocumentSubmissionExtractionSchemaTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static DocumentExtractionResult validate(String json) {
        return UserDocumentSubmissionExtractionSchema.validate(MAPPER.readTree(json));
    }

    @Test
    void acceptsTitleDescriptionAndTags() {
        DocumentExtractionResult result =
                validate(
                        """
                        {"title":" Erste-Hilfe-Bescheinigung ","description":"Nachweis","tags":["Erste Hilfe","Nachweis"]}
                        """);

        assertThat(result.fields())
                .containsEntry("title", "Erste-Hilfe-Bescheinigung")
                .containsEntry("description", "Nachweis")
                .containsEntry("tags", List.of("Erste Hilfe", "Nachweis"));
        assertThat(result.hints()).isEmpty();
    }

    @Test
    void dropsDuplicateAndSurplusTags() {
        DocumentExtractionResult result =
                validate(
                        """
                        {"title":null,"description":null,"tags":["a","A","b","c","d","e","f"]}
                        """);

        assertThat(result.fields()).containsEntry("tags", List.of("a", "b", "c", "d", "e"));
        assertThat(result.hints())
                .extracting(ExtractionHint::code)
                .containsExactly(ExtractionHint.VALUE_REJECTED);
    }

    @Test
    void reportsNothingRecognizedForEmptyAnswer() {
        DocumentExtractionResult result =
                validate("{\"title\":null,\"description\":null,\"tags\":[]}");

        assertThat(result.fields()).isEmpty();
        assertThat(result.hints())
                .extracting(ExtractionHint::code)
                .containsExactly(ExtractionHint.NOTHING_RECOGNIZED);
    }
}
