/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai.extraction;

import java.util.Map;
import java.util.function.Function;
import tools.jackson.databind.JsonNode;

/**
 * Everything the generic {@link DocumentExtractionService} needs for one extraction, prepared by
 * the domain in the user's context: prompt part, JSON schema (with the allowed catalog values) and
 * the deterministic validator for the model's answer.
 */
public record PreparedExtraction(
        String schemaName,
        String instruction,
        Map<String, Object> jsonSchema,
        Function<JsonNode, DocumentExtractionResult> validator) {}
