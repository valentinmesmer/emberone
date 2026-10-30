/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.service;

import com.emberone.notification.config.NotificationProperties;
import com.emberone.notification.domain.NotificationDelivery;
import com.emberone.notification.domain.NotificationDeliveryStatus;
import com.emberone.notification.repository.NotificationDeliveryRepository;
import com.emberone.platform.outbox.OutboxBackoff;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class NotificationOutboxTransactionService {

    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> claimPendingDeliveryIds(Instant now, Instant staleBefore, int limit) {
        return deliveryRepository.claimPendingDeliveryIds(now, staleBefore, limit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<NotificationDelivery> loadClaimedDeliveries(List<Long> deliveryIds) {
        return deliveryRepository.findByIdInWithNotification(deliveryIds);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(Long deliveryId, int attempts) {
        NotificationDelivery delivery = deliveryRepository.findById(deliveryId).orElse(null);
        if (delivery == null || delivery.getStatus() != NotificationDeliveryStatus.IN_PROGRESS) {
            return;
        }
        delivery.setAttempts(attempts);
        delivery.setStatus(NotificationDeliveryStatus.SENT);
        delivery.setSentAt(Instant.now());
        delivery.setLastError(null);
        delivery.setNextAttemptAt(null);
        delivery.setProcessingStartedAt(null);
        deliveryRepository.save(delivery);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long deliveryId, int attempts, String error, boolean retryable) {
        NotificationDelivery delivery = deliveryRepository.findById(deliveryId).orElse(null);
        if (delivery == null || delivery.getStatus() != NotificationDeliveryStatus.IN_PROGRESS) {
            return;
        }
        delivery.setAttempts(attempts);
        delivery.setLastError(error);
        delivery.setProcessingStartedAt(null);
        if (retryable) {
            delivery.setStatus(NotificationDeliveryStatus.PENDING);
            delivery.setNextAttemptAt(OutboxBackoff.nextAttemptAfter(attempts, retryBackoff()));
        } else {
            delivery.setStatus(NotificationDeliveryStatus.FAILED);
            delivery.setNextAttemptAt(null);
        }
        deliveryRepository.save(delivery);
    }

    private Duration retryBackoff() {
        return properties.getDeliveryRetryBackoff();
    }
}
