/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.service;

import com.emberone.platform.security.TenantContextHolder;
import com.emberone.platform.tenant.TenantBatchRunner;
import com.emberone.retention.RetentionRegistry;
import com.emberone.retention.spi.RetentionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetentionExecutionService {

    private final TenantBatchRunner tenantBatchRunner;
    private final RetentionRegistry retentionRegistry;
    private final RetentionHandlerInvoker retentionHandlerInvoker;

    public void runForAllTenants() {
        tenantBatchRunner.forEachTenant(tenantId -> runForCurrentTenant());
    }

    public void runForCurrentTenant() {
        for (RetentionHandler handler : retentionRegistry.all()) {
            try {
                int deleted = retentionHandlerInvoker.invoke(handler);
                if (deleted > 0) {
                    log.info(
                            "Retention {} deleted {} record(s) for tenant {}",
                            handler.category(),
                            deleted,
                            TenantContextHolder.getTenantId());
                }
            } catch (Exception e) {
                log.error("Retention handler {} failed for tenant context", handler.category(), e);
            }
        }
    }
}
