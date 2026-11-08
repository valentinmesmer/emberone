/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.namechange;

import com.emberone.platform.ai.extraction.DocumentExtractionResult;
import com.emberone.platform.ai.extraction.DocumentExtractionSchema;
import com.emberone.platform.ai.extraction.ExtractionHint;
import com.emberone.platform.ai.extraction.ExtractionJson;
import com.emberone.platform.ai.extraction.PreparedExtraction;
import com.emberone.workflow.domain.WorkflowType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Extraction schema for {@link WorkflowType#NAME_CHANGE}: the new name from a marriage certificate,
 * ID card or similar.
 */
@Component
public class NameChangeExtractionSchema implements DocumentExtractionSchema {

    @Override
    public String type() {
        return WorkflowType.NAME_CHANGE.name();
    }

    @Override
    public PreparedExtraction prepare(long userId, Long selectionId) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("firstName", ExtractionJson.nullableString("Given name(s) of the person."));
        properties.put("lastName", ExtractionJson.nullableString("Family name of the person."));
        return new PreparedExtraction(
                "name_change_extraction",
                "The document proves the person's current legal name (for example a marriage"
                        + " certificate, a name-change decision or an ID card). Extract the name"
                        + " the person carries now.",
                ExtractionJson.object(properties),
                NameChangeExtractionSchema::validate);
    }

    /** Deterministic plausibility rules; invalid values are dropped, never repaired. */
    static DocumentExtractionResult validate(JsonNode answer) {
        Map<String, Object> fields = new LinkedHashMap<>();
        ExtractionJson.text(answer.get("firstName"), 255)
                .ifPresent(v -> fields.put("firstName", v));
        ExtractionJson.text(answer.get("lastName"), 255).ifPresent(v -> fields.put("lastName", v));
        List<ExtractionHint> hints = new ArrayList<>();
        if (fields.isEmpty()) {
            hints.add(new ExtractionHint(ExtractionHint.NOTHING_RECOGNIZED, null));
        }
        return new DocumentExtractionResult(fields, hints);
    }
}
