/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai.extraction;

import java.util.List;
import java.util.Map;

/**
 * Validated suggestion for a form. {@code fields} holds only values that passed the deterministic
 * checks (keys are the form field names of the target request); rejected values are absent, not
 * repaired.
 */
public record DocumentExtractionResult(Map<String, Object> fields, List<ExtractionHint> hints) {}
