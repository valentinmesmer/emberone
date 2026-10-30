/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.service;

import com.emberone.identity.config.IdentityProviderProperties;
import com.emberone.identity.domain.IdentityUser;
import com.emberone.identity.provider.IdentityProviderUserProvisioner;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityDeprovisionService {

    private final IdentityDeprovisionTransactionService identityDeprovisionTransactionService;
    private final IdentityProviderUserProvisioner identityProviderUserProvisioner;
    private final IdentityProviderProperties properties;

    public void processPendingDeprovisions() {
        Instant now = Instant.now();
        Instant staleBefore = now.minus(properties.getDeprovisionProcessingStaleAfter());
        int batchSize = properties.getDeprovisionBatchSize();
        while (true) {
            List<Long> claimedIds =
                    identityDeprovisionTransactionService.claimPendingDeprovisionIds(
                            now, staleBefore, batchSize);
            if (claimedIds.isEmpty()) {
                return;
            }
            for (Long identityUserId : claimedIds) {
                processClaimedDeprovision(identityUserId);
            }
        }
    }

    private void processClaimedDeprovision(Long identityUserId) {
        IdentityUser identityUser =
                identityDeprovisionTransactionService.loadClaimedIdentityUser(identityUserId);
        if (identityUser == null || identityUser.getDeprovisionPendingAt() == null) {
            return;
        }
        try {
            if (StringUtils.hasText(identityUser.getIdpUserId())) {
                identityProviderUserProvisioner.deleteUser(identityUser.getIdpUserId());
            }
            identityDeprovisionTransactionService.completeDeprovision(identityUserId);
        } catch (Exception ex) {
            log.warn("Identity deprovision failed for idm_users.id={}", identityUserId, ex);
            identityDeprovisionTransactionService.recordDeprovisionFailure(
                    identityUserId, ex.getMessage());
        }
    }
}
