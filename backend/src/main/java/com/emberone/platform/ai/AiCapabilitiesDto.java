/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

import java.util.Map;

/**
 * Which AI functions the current user may use right now. The frontend shows buttons only when the
 * matching flag is true; the backend enforces the same rules again on every call.
 */
public record AiCapabilitiesDto(
        boolean enabled, FunctionCapability helpChat, DocumentExtraction documentExtraction) {

    public record FunctionCapability(boolean available) {}

    /**
     * @param types per proof type key (workflow type name)
     */
    public record DocumentExtraction(boolean available, Map<String, ProofType> types) {}

    /**
     * @param available extraction is available for this proof type
     */
    public record ProofType(boolean available) {}
}
