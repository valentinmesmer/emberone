/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.tenant;

import com.emberone.admin.tenant.repository.TenantRepository;
import com.emberone.platform.logging.RequestIdSupport;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.platform.security.TenantContextSupport;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Runs batch jobs once per tenant with {@link TenantContextHolder} set so FORCE RLS policies apply.
 * Failures for one tenant are logged and do not prevent other tenants from running.
 */
@Service
@Slf4j
public class TenantBatchRunner {

    private final TenantRepository tenantRepository;

    public TenantBatchRunner(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public void forEachTenant(Consumer<Long> action) {
        for (Long tenantId : tenantRepository.findAllIdsOrderByIdAsc()) {
            try {
                RequestIdSupport.putMdc(RequestIdSupport.generate());
                TenantContextHolder.setTenantId(tenantId);
                TenantContextSupport.syncMdcFromHolder();
                action.accept(tenantId);
            } catch (Exception e) {
                log.error("Tenant batch job failed for tenant {}", tenantId, e);
            } finally {
                TenantContextSupport.clearMdc();
                RequestIdSupport.clearMdc();
                TenantContextHolder.clear();
            }
        }
    }
}
