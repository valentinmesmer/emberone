/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

import com.emberone.platform.security.TokenService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The only way to the model. Order: availability guard, per-user rate limit, call, parse. It holds
 * no state about prompts or answers (no logging, no persistence); only content-free Micrometer
 * counters per function and outcome.
 *
 * <p>Callers must run in the user's security context; the gateway never loads data itself.
 */
@Service
@RequiredArgsConstructor
public class AiGateway {

    private final AiAvailabilityGuard guard;
    private final AiRateLimiter rateLimiter;
    private final OpenAiCompatibleClient client;
    private final TokenService tokenService;
    private final JsonMapper jsonMapper;
    private final MeterRegistry meterRegistry;

    /** Calls the model and returns its structured JSON answer (not yet validated by the caller). */
    public JsonNode complete(AiCompletionRequest request) {
        guard.require();
        rateLimiter.acquire(tokenService.requireUserId());
        try {
            AiCompletionResult result = client.complete(request);
            JsonNode node = parse(result.content());
            count(request.function(), "success");
            if (result.totalTokens() > 0) {
                meterRegistry
                        .counter("emberone.ai.tokens", "function", request.function())
                        .increment(result.totalTokens());
            }
            return node;
        } catch (AiProviderException ex) {
            count(request.function(), "error");
            throw ex;
        }
    }

    private void count(String function, String outcome) {
        meterRegistry
                .counter("emberone.ai.requests", "function", function, "outcome", outcome)
                .increment();
    }

    private JsonNode parse(String content) {
        try {
            JsonNode node = jsonMapper.readTree(stripCodeFence(content));
            if (node == null || !node.isObject()) {
                throw new AiProviderException("Model answer is not a JSON object");
            }
            return node;
        } catch (AiProviderException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new AiProviderException("Model answer is not valid JSON");
        }
    }

    private static String stripCodeFence(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}
