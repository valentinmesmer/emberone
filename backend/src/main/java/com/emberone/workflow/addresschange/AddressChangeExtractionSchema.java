/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.addresschange;

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
 * Extraction schema for {@link WorkflowType#ADDRESS_CHANGE}: the new address from a registration
 * certificate or ID card.
 */
@Component
public class AddressChangeExtractionSchema implements DocumentExtractionSchema {

    @Override
    public String type() {
        return WorkflowType.ADDRESS_CHANGE.name();
    }

    @Override
    public PreparedExtraction prepare(long userId, Long selectionId) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(
                "street",
                ExtractionJson.nullableString("Street name and house number of the address."));
        properties.put("postalCode", ExtractionJson.nullableString("Postal code."));
        properties.put("city", ExtractionJson.nullableString("City or town."));
        properties.put("country", ExtractionJson.nullableString("Country, in German."));
        return new PreparedExtraction(
                "address_change_extraction",
                "The document proves the person's current residential address (for example a"
                        + " registration certificate or an ID card). Extract that address.",
                ExtractionJson.object(properties),
                AddressChangeExtractionSchema::validate);
    }

    /** Deterministic plausibility rules; invalid values are dropped, never repaired. */
    static DocumentExtractionResult validate(JsonNode answer) {
        Map<String, Object> fields = new LinkedHashMap<>();
        ExtractionJson.text(answer.get("street"), 255).ifPresent(v -> fields.put("street", v));
        ExtractionJson.text(answer.get("postalCode"), 32)
                .ifPresent(v -> fields.put("postalCode", v));
        ExtractionJson.text(answer.get("city"), 255).ifPresent(v -> fields.put("city", v));
        ExtractionJson.text(answer.get("country"), 255).ifPresent(v -> fields.put("country", v));
        List<ExtractionHint> hints = new ArrayList<>();
        if (fields.isEmpty()) {
            hints.add(new ExtractionHint(ExtractionHint.NOTHING_RECOGNIZED, null));
        }
        return new DocumentExtractionResult(fields, hints);
    }
}
