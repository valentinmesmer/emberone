/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.namechange;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.platform.ai.extraction.DocumentExtractionResult;
import com.emberone.platform.ai.extraction.ExtractionHint;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class NameChangeExtractionSchemaTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Test
    void acceptsName() {
        DocumentExtractionResult result =
                NameChangeExtractionSchema.validate(
                        MAPPER.readTree("{\"firstName\":\"Anna\",\"lastName\":\"Meier\"}"));

        assertThat(result.fields())
                .containsEntry("firstName", "Anna")
                .containsEntry("lastName", "Meier");
        assertThat(result.hints()).isEmpty();
    }

    @Test
    void reportsNothingRecognized() {
        DocumentExtractionResult result =
                NameChangeExtractionSchema.validate(
                        MAPPER.readTree("{\"firstName\":null,\"lastName\":null}"));

        assertThat(result.fields()).isEmpty();
        assertThat(result.hints())
                .extracting(ExtractionHint::code)
                .containsExactly(ExtractionHint.NOTHING_RECOGNIZED);
    }
}
