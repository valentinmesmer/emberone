/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.publish;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.emberone.platform.realtime.event.RealtimeEvent;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class RealtimePublisherTest {

    @Mock private RealtimeRedisFanOut redisFanOut;

    private RealtimePublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new RealtimePublisher(redisFanOut);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void publishToUserSendsToPrincipalQueueImmediatelyWithoutTransaction() {
        RealtimeEvent event =
                RealtimeEvent.withPayload(
                        "notification.created", Map.of("id", 1L), Map.of("id", 1L));

        publisher.publishToUser(3L, 9L, RealtimePublisher.NOTIFICATIONS_QUEUE, event);

        verify(redisFanOut).publishToUser(3L, 9L, RealtimePublisher.NOTIFICATIONS_QUEUE, event);
    }

    @Test
    void publishToUserDefersUntilAfterCommitWhenTransactionActive() {
        TransactionSynchronizationManager.initSynchronization();
        RealtimeEvent event = RealtimeEvent.thin("todo.created", Map.of("id", 2L));

        publisher.publishToUser(3L, 9L, RealtimePublisher.TODOS_QUEUE, event);

        verify(redisFanOut, never())
                .publishToUser(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any());

        for (TransactionSynchronization synchronization :
                TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }

        verify(redisFanOut).publishToUser(3L, 9L, RealtimePublisher.TODOS_QUEUE, event);
    }

    @Test
    void publishToTopicBuildsTenantDestinationForThinEvent() {
        RealtimeEvent event = RealtimeEvent.thin("workflow.updated", Map.of("workflowId", 12L));

        publisher.publishToTopic(4L, "workflow.12", event);

        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisFanOut)
                .publishToTopic(
                        destinationCaptor.capture(), org.mockito.ArgumentMatchers.eq(event));
        assertThat(destinationCaptor.getValue()).isEqualTo("/topic/tenant.4.workflow.12");
    }

    @Test
    void publishToTopicRejectsPayloadEvents() {
        RealtimeEvent event =
                RealtimeEvent.withPayload("workflow.updated", Map.of("workflowId", 12L), Map.of());

        assertThatThrownBy(() -> publisher.publishToTopic(4L, "workflow.12", event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("thin");
    }
}
