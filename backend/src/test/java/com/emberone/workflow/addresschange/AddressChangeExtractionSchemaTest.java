/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.addresschange;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.platform.ai.extraction.DocumentExtractionResult;
import com.emberone.platform.ai.extraction.ExtractionHint;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class AddressChangeExtractionSchemaTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Test
    void acceptsReadableAddressAndDropsEmptyValues() {
        DocumentExtractionResult result =
                AddressChangeExtractionSchema.validate(
                        MAPPER.readTree(
                                """
                                {"street":" Hauptstr. 1 ","postalCode":"80331","city":"München","country":""}
                                """));

        assertThat(result.fields())
                .containsEntry("street", "Hauptstr. 1")
                .containsEntry("postalCode", "80331")
                .containsEntry("city", "München")
                .doesNotContainKey("country");
        assertThat(result.hints()).isEmpty();
    }

    @Test
    void reportsNothingRecognized() {
        DocumentExtractionResult result =
                AddressChangeExtractionSchema.validate(
                        MAPPER.readTree(
                                "{\"street\":null,\"postalCode\":null,\"city\":null,\"country\":null}"));

        assertThat(result.fields()).isEmpty();
        assertThat(result.hints())
                .extracting(ExtractionHint::code)
                .containsExactly(ExtractionHint.NOTHING_RECOGNIZED);
    }
}
