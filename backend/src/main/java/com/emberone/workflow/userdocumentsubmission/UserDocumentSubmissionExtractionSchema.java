/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.userdocumentsubmission;

import com.emberone.platform.ai.extraction.DocumentExtractionResult;
import com.emberone.platform.ai.extraction.DocumentExtractionSchema;
import com.emberone.platform.ai.extraction.ExtractionHint;
import com.emberone.platform.ai.extraction.ExtractionJson;
import com.emberone.platform.ai.extraction.PreparedExtraction;
import com.emberone.workflow.domain.WorkflowType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Extraction schema for {@link WorkflowType#USER_DOCUMENT_SUBMISSION}: a short title, a one-line
 * description and a few tags. There is no catalog, so no selection is needed.
 */
@Component
public class UserDocumentSubmissionExtractionSchema implements DocumentExtractionSchema {

    static final int MAX_TAGS = 5;
    private static final int MAX_TITLE = 512;
    private static final int MAX_DESCRIPTION = 2048;
    private static final int MAX_TAG = 128;

    @Override
    public String type() {
        return WorkflowType.USER_DOCUMENT_SUBMISSION.name();
    }

    @Override
    public PreparedExtraction prepare(long userId, Long selectionId) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(
                "title",
                ExtractionJson.nullableString(
                        "Short, descriptive document title (for example the document kind and"
                                + " subject), at most 100 characters."));
        properties.put(
                "description",
                ExtractionJson.nullableString(
                        "One or two sentences on what the document is. Null if unclear."));
        properties.put(
                "tags",
                ExtractionJson.arrayOf(
                        Map.of("type", "string"),
                        "Up to " + MAX_TAGS + " short keywords (one or two words each)."));
        return new PreparedExtraction(
                "user_document_submission_extraction",
                "The document is a personal document that a member of a fire department submits to"
                        + " their archive. Write title, description and tags in German.",
                ExtractionJson.object(properties),
                UserDocumentSubmissionExtractionSchema::validate);
    }

    /** Deterministic plausibility rules; invalid values are dropped, never repaired. */
    static DocumentExtractionResult validate(JsonNode answer) {
        Map<String, Object> fields = new LinkedHashMap<>();
        List<ExtractionHint> hints = new ArrayList<>();

        ExtractionJson.text(answer.get("title"), MAX_TITLE)
                .ifPresent(value -> fields.put("title", value));
        ExtractionJson.text(answer.get("description"), MAX_DESCRIPTION)
                .ifPresent(value -> fields.put("description", value));

        JsonNode tags = answer.get("tags");
        if (tags != null && tags.isArray()) {
            Set<String> seen = new LinkedHashSet<>();
            List<String> accepted = new ArrayList<>();
            boolean rejected = false;
            for (JsonNode node : tags) {
                var tag = ExtractionJson.text(node, MAX_TAG);
                if (tag.isEmpty() || !seen.add(tag.get().toLowerCase(Locale.ROOT))) {
                    rejected = true;
                    continue;
                }
                if (accepted.size() >= MAX_TAGS) {
                    rejected = true;
                    continue;
                }
                accepted.add(tag.get());
            }
            if (!accepted.isEmpty()) {
                fields.put("tags", accepted);
            }
            if (rejected) {
                hints.add(new ExtractionHint(ExtractionHint.VALUE_REJECTED, "tags"));
            }
        }

        if (fields.isEmpty()) {
            hints.add(new ExtractionHint(ExtractionHint.NOTHING_RECOGNIZED, null));
        }
        return new DocumentExtractionResult(fields, hints);
    }
}
