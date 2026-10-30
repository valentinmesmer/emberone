/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.service;

import com.emberone.admin.tenant.domain.Tenant;
import com.emberone.admin.tenant.dto.PurgeTenantRequest;
import com.emberone.admin.tenant.repository.TenantRepository;
import com.emberone.identity.service.IdentityUserService;
import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.storage.service.ObjectStorageBucketCatalog;
import com.emberone.platform.storage.service.ObjectStorageDeletionOutboxService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Irreversible tenant purge for shared-instance operators. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTenantPurgeService {

    private final TenantRepository tenantRepository;
    private final ObjectStorageBucketCatalog objectStorageBucketCatalog;
    private final ObjectStorageDeletionOutboxService objectStorageDeletionOutboxService;
    private final TokenService tokenService;
    private final IdentityUserService identityUserService;
    private final AdminTenantPurgeTransactionService purgeTransactionService;

    public void purgeTenant(long tenantId, PurgeTenantRequest request) {
        TenantScopeGuard.requireActiveTenantMatches(tenantId);
        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Tenant not found"));

        String confirmed = request.getConfirmationName().trim();
        if (!tenant.getName().equals(confirmed)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Confirmation does not match tenant name");
        }

        Long actorUserId = tokenService.getUserId();

        String tenantPrefix = "tenants/" + tenantId + "/";
        for (String bucket : objectStorageBucketCatalog.tenantScopedBuckets()) {
            objectStorageDeletionOutboxService.enqueuePrefixDeletion(
                    tenantId, bucket, tenantPrefix);
        }

        identityUserService.deprovisionAllIdpUsersForTenantPurge(tenantId);

        purgeTransactionService.purgeTenantData(tenantId);
        purgeTransactionService.deleteTenant(tenant);

        log.info(
                "Tenant purged tenantId={} actorUserId={} purgedAt={}",
                tenantId,
                actorUserId,
                Instant.now());
    }
}
