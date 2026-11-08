/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

/** Raw model output (JSON text) plus token usage when the provider reports it. */
public record AiCompletionResult(String content, long totalTokens) {}
