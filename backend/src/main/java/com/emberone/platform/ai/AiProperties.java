/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Operator configuration of the optional AI assistance. Everything is off by default.
 *
 * <p>Prototype: the switch is global (env), not per tenant (that needs a migration; see concept
 * doc).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "emberone.ai")
public class AiProperties {

    /** Global switch; when false no AI function is available. */
    private boolean enabled = false;

    /** Base URL of an OpenAI-compatible API, including {@code /v1}. */
    private String baseUrl = "http://localhost:11434/v1";

    /** Optional bearer token for the endpoint (never logged). */
    private String apiKey;

    private Models models = new Models();

    /**
     * How structured output is requested: {@code JSON_SCHEMA} ({@code response_format} with schema,
     * supported by Ollama and vLLM) or {@code JSON_OBJECT} (schema only in the prompt, for
     * providers without schema support).
     */
    private StructuredOutputMode structuredOutput = StructuredOutputMode.JSON_SCHEMA;

    /** Total request timeout towards the model endpoint. */
    private Duration timeout = Duration.ofSeconds(120);

    /** Calls per user and minute (in-memory, per instance). */
    private int rateLimitPerMinute = 20;

    /**
     * Sent as {@code reasoning_effort}. Default {@code none}: reasoning models such as gemma4 on
     * Ollama otherwise think first and cut off the structured answer. Blank = not sent (for
     * providers/models that reject the parameter).
     */
    private String reasoningEffort = "none";

    public enum StructuredOutputMode {
        JSON_SCHEMA,
        JSON_OBJECT
    }

    @Getter
    @Setter
    public static class Models {
        private String text = "gemma4";
        private String vision = "gemma4";
    }
}
