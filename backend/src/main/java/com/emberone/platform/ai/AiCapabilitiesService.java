/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

import com.emberone.platform.ai.extraction.DocumentExtractionSchema;
import com.emberone.platform.ai.extraction.DocumentExtractionService;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiCapabilitiesService {

    private final AiProperties properties;
    private final AiAvailabilityGuard guard;
    private final DocumentExtractionService extractionService;

    public AiCapabilitiesDto capabilitiesForCurrentUser() {
        boolean helpChat = guard.isAvailable();
        boolean extraction = guard.isAvailable();
        Map<String, AiCapabilitiesDto.ProofType> types = new LinkedHashMap<>();
        if (extraction) {
            for (DocumentExtractionSchema schema : extractionService.schemas()) {
                types.put(schema.type(), new AiCapabilitiesDto.ProofType(true));
            }
        }
        return new AiCapabilitiesDto(
                properties.isEnabled(),
                new AiCapabilitiesDto.FunctionCapability(helpChat),
                new AiCapabilitiesDto.DocumentExtraction(extraction, types));
    }
}
