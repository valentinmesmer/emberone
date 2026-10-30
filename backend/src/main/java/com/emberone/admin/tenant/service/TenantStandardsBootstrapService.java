/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.service;

import com.emberone.platform.security.TenantContextHolder;
import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.standard.service.StandardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds or re-syncs the tenant baseline (catalogs, SYSTEM standard) for a tenant that exists in
 * {@code tenants}. Runs in its own transaction with the target tenant bound for RLS.
 */
@Service
@RequiredArgsConstructor
public class TenantStandardsBootstrapService {

    private final StandardService standardService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncBaseline(long tenantId) {
        TenantScopeGuard.bind(tenantId);
        try {
            standardService.ensureTenantBaselineSynced();
        } finally {
            TenantContextHolder.clear();
        }
    }
}
