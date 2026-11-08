/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.rankrequest;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.platform.ai.extraction.DocumentExtractionResult;
import com.emberone.platform.ai.extraction.ExtractionHint;
import com.emberone.rank.dto.TenantRankDto;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class RankRequestExtractionSchemaTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final Map<String, TenantRankDto> CATALOG =
            Map.of(
                    "OBM",
                    TenantRankDto.builder()
                            .id(7L)
                            .configKey("OBM")
                            .name("Oberbrandmeister")
                            .build());

    @Test
    void mapsCatalogKeyToRankId() {
        DocumentExtractionResult result =
                RankRequestExtractionSchema.validate(
                        MAPPER.readTree("{\"rank\":\"OBM\"}"), CATALOG);

        assertThat(result.fields()).containsEntry("tenantRankId", 7L);
        assertThat(result.hints()).isEmpty();
    }

    @Test
    void rejectsUnknownRank() {
        DocumentExtractionResult result =
                RankRequestExtractionSchema.validate(
                        MAPPER.readTree("{\"rank\":\"XYZ\"}"), CATALOG);

        assertThat(result.fields()).isEmpty();
        assertThat(result.hints())
                .extracting(ExtractionHint::code)
                .containsExactly(ExtractionHint.VALUE_REJECTED, ExtractionHint.NOTHING_RECOGNIZED);
    }
}
