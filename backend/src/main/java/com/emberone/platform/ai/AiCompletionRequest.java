/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

import java.util.List;
import java.util.Map;

/**
 * One structured model call. {@code function} is a fixed low-cardinality label for metrics (never
 * user content). {@code images} are attached to the last message.
 */
public record AiCompletionRequest(
        String function,
        AiModel model,
        String systemPrompt,
        List<AiChatMessage> messages,
        List<AiImage> images,
        String schemaName,
        Map<String, Object> jsonSchema) {

    public AiCompletionRequest {
        images = images == null ? List.of() : List.copyOf(images);
        messages = List.copyOf(messages);
    }
}
