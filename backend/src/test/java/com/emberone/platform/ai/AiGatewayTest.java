/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.platform.security.TokenService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class AiGatewayTest {

    @Mock OpenAiCompatibleClient client;
    @Mock TokenService tokenService;

    AiProperties properties = new AiProperties();
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    AiGateway gateway;

    @BeforeEach
    void setUp() {
        properties.setEnabled(true);
        gateway =
                new AiGateway(
                        new AiAvailabilityGuard(properties),
                        new AiRateLimiter(properties),
                        client,
                        tokenService,
                        JsonMapper.builder().build(),
                        meters);
    }

    private static AiCompletionRequest request() {
        return new AiCompletionRequest(
                "unit",
                AiModel.TEXT,
                "system",
                List.of(AiChatMessage.user("hi")),
                List.of(),
                "x",
                Map.of("type", "object"));
    }

    @Test
    void returnsParsedJsonAndCounts() {
        when(tokenService.requireUserId()).thenReturn(1L);
        when(client.complete(any())).thenReturn(new AiCompletionResult("{\"a\":1}", 12));

        JsonNode node = gateway.complete(request());

        assertThat(node.get("a").asInt()).isEqualTo(1);
        assertThat(
                        meters.counter(
                                        "emberone.ai.requests",
                                        "function",
                                        "unit",
                                        "outcome",
                                        "success")
                                .count())
                .isEqualTo(1.0);
        assertThat(meters.counter("emberone.ai.tokens", "function", "unit").count())
                .isEqualTo(12.0);
    }

    @Test
    void toleratesMarkdownCodeFence() {
        when(tokenService.requireUserId()).thenReturn(1L);
        when(client.complete(any()))
                .thenReturn(new AiCompletionResult("```json\n{\"a\":2}\n```", 0));

        assertThat(gateway.complete(request()).get("a").asInt()).isEqualTo(2);
    }

    @Test
    void nonJsonAnswerIsProviderError() {
        when(tokenService.requireUserId()).thenReturn(1L);
        when(client.complete(any())).thenReturn(new AiCompletionResult("sorry, no json", 0));

        assertThatThrownBy(() -> gateway.complete(request()))
                .isInstanceOf(AiProviderException.class)
                .hasMessageNotContaining("sorry");
    }

    @Test
    void disabledNeverReachesTheModel() {
        properties.setEnabled(false);

        assertThatThrownBy(() -> gateway.complete(request()))
                .isInstanceOf(AiUnavailableException.class);

        verify(client, never()).complete(any());
    }

    @Test
    void rateLimitIsEnforcedPerUser() {
        properties.setRateLimitPerMinute(1);
        when(tokenService.requireUserId()).thenReturn(1L);
        when(client.complete(any())).thenReturn(new AiCompletionResult("{}", 0));

        gateway.complete(request());

        assertThatThrownBy(() -> gateway.complete(request()))
                .isInstanceOf(AiRateLimitException.class);
    }
}
