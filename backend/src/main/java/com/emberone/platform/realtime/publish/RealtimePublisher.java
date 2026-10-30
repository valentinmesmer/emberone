/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.realtime.publish;

import com.emberone.platform.realtime.event.RealtimeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "emberone.realtime", name = "enabled", havingValue = "true")
public class RealtimePublisher {

    public static final String NOTIFICATIONS_QUEUE = "/queue/notifications";
    public static final String TODOS_QUEUE = "/queue/todos";
    public static final String PERMISSIONS_QUEUE = "/queue/permissions";
    public static final String WORKFLOWS_QUEUE = "/queue/workflows";

    private final RealtimeRedisFanOut redisFanOut;

    public void publishToUser(Long tenantId, Long userId, String userQueue, RealtimeEvent event) {
        Assert.notNull(tenantId, "tenantId");
        Assert.notNull(userId, "userId");
        Assert.hasText(userQueue, "userQueue");
        Assert.notNull(event, "event");
        runAfterCommit(() -> redisFanOut.publishToUser(tenantId, userId, userQueue, event));
    }

    public void publishToTopic(Long tenantId, String topicSuffix, RealtimeEvent event) {
        Assert.notNull(tenantId, "tenantId");
        Assert.hasText(topicSuffix, "topicSuffix");
        Assert.notNull(event, "event");
        Assert.isTrue(event.isThin(), "Topic events must be thin (no payload)");
        String destination = "/topic/tenant." + tenantId + "." + topicSuffix;
        runAfterCommit(() -> redisFanOut.publishToTopic(destination, event));
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            action.run();
                        }
                    });
            return;
        }
        action.run();
    }
}
