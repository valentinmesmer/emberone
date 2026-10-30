/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.service;

import com.emberone.identity.config.IdentityProviderProperties;
import com.emberone.identity.domain.IdentityUser;
import com.emberone.identity.repository.IdentityUserRepository;
import com.emberone.platform.outbox.OutboxBackoff;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
class IdentityDeprovisionTransactionService {

    private final IdentityUserRepository identityUserRepository;
    private final IdentityProviderProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> claimPendingDeprovisionIds(Instant now, Instant staleBefore, int limit) {
        return identityUserRepository.claimPendingDeprovisionIds(
                now, staleBefore, limit, properties.getMaxDeprovisionAttempts());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public IdentityUser loadClaimedIdentityUser(Long identityUserId) {
        return identityUserRepository.findById(identityUserId).orElse(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeDeprovision(Long identityUserId) {
        IdentityUser identityUser = identityUserRepository.findById(identityUserId).orElse(null);
        if (identityUser == null || identityUser.getDeprovisionPendingAt() == null) {
            return;
        }
        identityUser.setIdpUserId(null);
        identityUser.setDeprovisionPendingAt(null);
        identityUser.setDeprovisionAttempts(0);
        identityUser.setDeprovisionLastError(null);
        identityUser.setDeprovisionNextAttemptAt(null);
        identityUser.setDeprovisionProcessingStartedAt(null);
        identityUserRepository.save(identityUser);
        log.info("Deprovisioned identity for user {}", identityUser.getEmberoneUserId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDeprovisionFailure(Long identityUserId, String error) {
        IdentityUser identityUser = identityUserRepository.findById(identityUserId).orElse(null);
        if (identityUser == null || identityUser.getDeprovisionPendingAt() == null) {
            return;
        }
        int attempts = identityUser.getDeprovisionAttempts() + 1;
        identityUser.setDeprovisionAttempts(attempts);
        identityUser.setDeprovisionLastError(error);
        identityUser.setDeprovisionProcessingStartedAt(null);
        boolean retryable = attempts < properties.getMaxDeprovisionAttempts();
        if (retryable) {
            identityUser.setDeprovisionNextAttemptAt(
                    OutboxBackoff.nextAttemptAfter(attempts, retryBackoff()));
        } else {
            identityUser.setDeprovisionNextAttemptAt(null);
            log.error(
                    "Identity deprovision for idm_users.id={} permanently failed after {} attempts: {}",
                    identityUserId,
                    attempts,
                    error);
        }
        identityUserRepository.save(identityUser);
    }

    private Duration retryBackoff() {
        return properties.getDeprovisionRetryBackoff();
    }
}
