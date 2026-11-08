/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Slim client for the OpenAI-compatible {@code /chat/completions} API (Ollama, vLLM, llama.cpp
 * server, EU providers). No SDK dependency. Prompts and answers are never logged; error messages
 * contain no content.
 */
@Component
@Slf4j
public class OpenAiCompatibleClient {

    private final AiProperties properties;
    private final JsonMapper jsonMapper;
    private final RestClient restClient;

    public OpenAiCompatibleClient(AiProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        HttpClient httpClient =
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(Duration.ofSeconds(5))
                        .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getTimeout());
        RestClient.Builder builder = RestClient.builder().requestFactory(requestFactory);
        if (StringUtils.hasText(properties.getBaseUrl())) {
            builder.baseUrl(stripTrailingSlash(properties.getBaseUrl()));
        }
        this.restClient = builder.build();
    }

    public AiCompletionResult complete(AiCompletionRequest request) {
        String body;
        try {
            body = jsonMapper.writeValueAsString(buildBody(request));
        } catch (RuntimeException ex) {
            throw new AiProviderException("Could not serialize model request");
        }
        String response;
        try {
            RestClient.RequestBodySpec spec =
                    restClient
                            .post()
                            .uri("/chat/completions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON);
            if (StringUtils.hasText(properties.getApiKey())) {
                spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey());
            }
            response = spec.body(body).retrieve().body(String.class);
        } catch (RestClientResponseException ex) {
            // Client errors (4xx) name the rejected parameter; the body is cut short and never
            // contains the document. Other messages are not logged: they may echo the exchange.
            if (ex.getStatusCode().is4xxClientError()) {
                log.warn(
                        "Model endpoint rejected the request ({}): {}",
                        ex.getStatusCode().value(),
                        abbreviate(ex.getResponseBodyAsString()));
            }
            throw new AiProviderException(
                    "Model endpoint call failed (" + ex.getStatusCode().value() + ")", ex);
        } catch (RestClientException ex) {
            // Do not include ex.getMessage(): it may echo parts of the exchange.
            throw new AiProviderException(
                    "Model endpoint call failed (" + ex.getClass().getSimpleName() + ")", ex);
        }
        return parseResponse(response);
    }

    private static String abbreviate(String body) {
        if (body == null) {
            return "";
        }
        String flat = body.replaceAll("\\s+", " ").trim();
        return flat.length() > 300 ? flat.substring(0, 300) + "…" : flat;
    }

    /** Builds the request body; package-private for tests. */
    Map<String, Object> buildBody(AiCompletionRequest request) {
        boolean schemaMode =
                properties.getStructuredOutput() == AiProperties.StructuredOutputMode.JSON_SCHEMA;
        String system = request.systemPrompt();
        if (!schemaMode) {
            system =
                    system
                            + "\n\nRespond with a single JSON object that conforms to this JSON"
                            + " schema and nothing else:\n"
                            + jsonMapper.writeValueAsString(request.jsonSchema());
        }
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", system));
        for (int i = 0; i < request.messages().size(); i++) {
            AiChatMessage message = request.messages().get(i);
            boolean last = i == request.messages().size() - 1;
            if (last && !request.images().isEmpty()) {
                List<Map<String, Object>> parts = new ArrayList<>();
                parts.add(Map.of("type", "text", "text", message.content()));
                for (AiImage image : request.images()) {
                    parts.add(
                            Map.of(
                                    "type",
                                    "image_url",
                                    "image_url",
                                    Map.of("url", image.toDataUrl())));
                }
                messages.add(Map.of("role", message.role(), "content", parts));
            } else {
                messages.add(Map.of("role", message.role(), "content", message.content()));
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(
                "model",
                request.model() == AiModel.VISION
                        ? properties.getModels().getVision()
                        : properties.getModels().getText());
        body.put("stream", false);
        body.put("temperature", 0);
        if (StringUtils.hasText(properties.getReasoningEffort())) {
            body.put("reasoning_effort", properties.getReasoningEffort().trim());
        }
        body.put("messages", messages);
        if (schemaMode) {
            body.put(
                    "response_format",
                    Map.of(
                            "type",
                            "json_schema",
                            "json_schema",
                            Map.of(
                                    "name",
                                    request.schemaName(),
                                    "strict",
                                    true,
                                    "schema",
                                    request.jsonSchema())));
        } else {
            body.put("response_format", Map.of("type", "json_object"));
        }
        return body;
    }

    private AiCompletionResult parseResponse(String response) {
        if (!StringUtils.hasText(response)) {
            throw new AiProviderException("Model endpoint returned an empty response");
        }
        try {
            JsonNode root = jsonMapper.readTree(response);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isString() || !StringUtils.hasText(content.asString())) {
                throw new AiProviderException("Model endpoint returned no message content");
            }
            long tokens = root.path("usage").path("total_tokens").asLong(0L);
            return new AiCompletionResult(content.asString(), tokens);
        } catch (AiProviderException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new AiProviderException("Model endpoint returned an unreadable response");
        }
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
