/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@SuppressWarnings("unchecked")
class OpenAiCompatibleClientTest {

    private static final Map<String, Object> SCHEMA = Map.of("type", "object");

    private static AiCompletionRequest request(List<AiImage> images) {
        return new AiCompletionRequest(
                "test",
                images.isEmpty() ? AiModel.TEXT : AiModel.VISION,
                "system",
                List.of(AiChatMessage.user("question")),
                images,
                "answer",
                SCHEMA);
    }

    private static OpenAiCompatibleClient client(AiProperties properties) {
        return new OpenAiCompatibleClient(properties, JsonMapper.builder().build());
    }

    @Test
    void schemaModeSendsResponseFormatWithStrictSchema() {
        AiProperties properties = new AiProperties();
        properties.getModels().setText("text-model");

        Map<String, Object> body = client(properties).buildBody(request(List.of()));

        assertThat(body.get("model")).isEqualTo("text-model");
        assertThat(body.get("stream")).isEqualTo(false);
        Map<String, Object> format = (Map<String, Object>) body.get("response_format");
        assertThat(format.get("type")).isEqualTo("json_schema");
        Map<String, Object> jsonSchema = (Map<String, Object>) format.get("json_schema");
        assertThat(jsonSchema.get("name")).isEqualTo("answer");
        assertThat(jsonSchema.get("strict")).isEqualTo(true);
        assertThat(jsonSchema.get("schema")).isEqualTo(SCHEMA);
    }

    @Test
    void reasoningEffortIsSentByDefaultAndOmittedWhenBlank() {
        AiProperties properties = new AiProperties();

        assertThat(client(properties).buildBody(request(List.of())))
                .containsEntry("reasoning_effort", "none");

        properties.setReasoningEffort(" ");
        assertThat(client(properties).buildBody(request(List.of())))
                .doesNotContainKey("reasoning_effort");
    }

    @Test
    void objectModePutsSchemaIntoSystemPrompt() {
        AiProperties properties = new AiProperties();
        properties.setStructuredOutput(AiProperties.StructuredOutputMode.JSON_OBJECT);

        Map<String, Object> body = client(properties).buildBody(request(List.of()));

        assertThat((Map<String, Object>) body.get("response_format"))
                .containsEntry("type", "json_object");
        List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
        assertThat((String) messages.getFirst().get("content")).contains("\"type\"");
    }

    @Test
    void imagesAreAttachedToLastMessageAsDataUrl() {
        AiProperties properties = new AiProperties();
        properties.getModels().setVision("vision-model");

        Map<String, Object> body =
                client(properties)
                        .buildBody(request(List.of(new AiImage("image/jpeg", new byte[] {1, 2}))));

        assertThat(body.get("model")).isEqualTo("vision-model");
        List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
        List<Map<String, Object>> parts =
                (List<Map<String, Object>>) messages.getLast().get("content");
        assertThat(parts.get(0)).containsEntry("type", "text");
        Map<String, Object> imageUrl = (Map<String, Object>) parts.get(1).get("image_url");
        assertThat((String) imageUrl.get("url")).startsWith("data:image/jpeg;base64,");
    }
}
