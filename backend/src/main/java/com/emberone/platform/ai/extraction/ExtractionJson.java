/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai.extraction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import tools.jackson.databind.JsonNode;

/** Small helpers for building strict JSON schemas and reading model answers defensively. */
public final class ExtractionJson {

    private ExtractionJson() {}

    /** Object schema with all given properties required and no additional properties. */
    public static Map<String, Object> object(Map<String, Object> properties) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", new ArrayList<>(properties.keySet()));
        schema.put("properties", properties);
        return schema;
    }

    public static Map<String, Object> nullableString(String description) {
        return Map.of("type", List.of("string", "null"), "description", description);
    }

    public static Map<String, Object> nullableDate(String description) {
        return nullableString(description + " Format YYYY-MM-DD, or null if not clearly readable.");
    }

    public static Map<String, Object> nullableNumber(String description) {
        return Map.of("type", List.of("number", "null"), "description", description);
    }

    /** String restricted to the given values, or null. */
    public static Map<String, Object> nullableEnum(Collection<String> values, String description) {
        List<Object> allowed = new ArrayList<>(values);
        allowed.add(null);
        return Map.of(
                "type", List.of("string", "null"), "enum", allowed, "description", description);
    }

    public static Map<String, Object> arrayOf(Map<String, Object> items, String description) {
        return Map.of("type", "array", "items", items, "description", description);
    }

    public static Map<String, Object> enumString(Collection<String> values) {
        return Map.of("type", "string", "enum", new ArrayList<>(values));
    }

    /** Strict ISO date; anything else (including other formats) yields empty - never repaired. */
    public static Optional<LocalDate> date(JsonNode node) {
        if (node == null || !node.isString()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(node.asString().trim()));
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    /** Trimmed text within the length limit; empty when blank or too long (not truncated). */
    public static Optional<String> text(JsonNode node, int maxLength) {
        if (node == null || !node.isString()) {
            return Optional.empty();
        }
        String value = node.asString().trim();
        if (value.isEmpty() || value.length() > maxLength) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    /** One line per catalog entry for the prompt: {@code - KEY: name}. */
    public static String catalogList(Map<String, String> keyToName) {
        StringBuilder out = new StringBuilder();
        keyToName.forEach((key, name) ->
                out.append("- ")
                        .append(key)
                        .append(": ")
                        .append(name == null ? "" : name.replaceAll("[\\r\\n]+", " ").trim())
                        .append('\n'));
        return out.toString();
    }

    public static Optional<BigDecimal> number(JsonNode node) {
        if (node == null || !node.isNumber()) {
            return Optional.empty();
        }
        return Optional.of(node.decimalValue());
    }
}
