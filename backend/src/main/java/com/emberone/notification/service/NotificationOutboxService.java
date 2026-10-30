/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.service;

import com.emberone.notification.channel.ChannelSender;
import com.emberone.notification.channel.RecipientNotReachableException;
import com.emberone.notification.config.NotificationProperties;
import com.emberone.notification.domain.NotificationDelivery;
import com.emberone.notification.domain.NotificationDeliveryStatus;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOutboxService {

    private final NotificationOutboxTransactionService transactionService;
    private final List<ChannelSender> channelSenders;
    private final NotificationProperties properties;

    public void processPendingDeliveries() {
        Instant now = Instant.now();
        Instant staleBefore = now.minus(properties.getProcessingStaleAfter());
        int batchSize = properties.getOutboxBatchSize();
        List<Long> claimedIds =
                transactionService.claimPendingDeliveryIds(now, staleBefore, batchSize);
        if (claimedIds.isEmpty()) {
            return;
        }
        List<NotificationDelivery> deliveries =
                transactionService.loadClaimedDeliveries(claimedIds);
        for (NotificationDelivery delivery : deliveries) {
            processClaimedDelivery(delivery);
        }
    }

    private void processClaimedDelivery(NotificationDelivery delivery) {
        if (delivery.getStatus() != NotificationDeliveryStatus.IN_PROGRESS) {
            return;
        }
        int attempt = delivery.getAttempts() + 1;
        ChannelSender sender =
                channelSenders.stream()
                        .filter(s -> s.supports(delivery.getChannel()))
                        .findFirst()
                        .orElse(null);
        if (sender == null) {
            transactionService.markFailed(
                    delivery.getId(),
                    attempt,
                    "No channel sender registered for " + delivery.getChannel(),
                    false);
            return;
        }

        try {
            sender.send(delivery.getNotification(), delivery);
            transactionService.markSent(delivery.getId(), attempt);
        } catch (RecipientNotReachableException ex) {
            transactionService.markFailed(delivery.getId(), attempt, ex.getMessage(), false);
        } catch (Exception ex) {
            log.warn(
                    "Notification delivery {} failed on attempt {}", delivery.getId(), attempt, ex);
            boolean retryable = attempt < properties.getMaxDeliveryAttempts();
            transactionService.markFailed(delivery.getId(), attempt, ex.getMessage(), retryable);
        }
    }
}
