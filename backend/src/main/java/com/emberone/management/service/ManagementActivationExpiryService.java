/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.service;

import com.emberone.management.domain.ManagementActivation;
import com.emberone.management.realtime.ManagementActivationRealtimeNotifier;
import com.emberone.management.repository.ManagementActivationRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends the expiry event once per activation when its validity ends. The marker is claimed
 * atomically in the database, so concurrent instances never send the same event twice. The access
 * itself needs no change: the access check already reads {@code validUntil}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManagementActivationExpiryService {

    private final ManagementActivationRepository activationRepository;
    private final ManagementActivationRealtimeNotifier notifier;

    @Transactional
    public void notifyExpiredActivations() {
        Instant now = Instant.now();
        List<Long> claimedIds = activationRepository.claimExpiredForNotification(now);
        List<ManagementActivation> expired =
                claimedIds.isEmpty() ? List.of() : activationRepository.findAllById(claimedIds);
        for (ManagementActivation activation : expired) {
            notifier.notifyExpired(activation);
        }
        if (!expired.isEmpty()) {
            log.info("Notified {} expired management activation(s)", expired.size());
        }
    }
}
