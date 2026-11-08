/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai.extraction;

/**
 * Plausibility note for the user and approver. {@code code} is a stable key the frontend maps to a
 * localized text; {@code field} is the form field it refers to (may be null). Hints never decide
 * anything.
 */
public record ExtractionHint(String code, String field) {

    public static final String FUTURE_DATE = "FUTURE_DATE";
    public static final String DATE_ORDER = "DATE_ORDER";
    public static final String VALIDITY_EXCEEDS_INTERVAL = "VALIDITY_EXCEEDS_INTERVAL";
    public static final String TYPE_MISMATCH = "TYPE_MISMATCH";
    public static final String VALUE_REJECTED = "VALUE_REJECTED";
    public static final String NOTHING_RECOGNIZED = "NOTHING_RECOGNIZED";
}
