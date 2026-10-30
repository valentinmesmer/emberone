/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.service;

import com.emberone.admin.tenant.domain.Tenant;
import com.emberone.admin.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists tenant master data. Kept separate from standards bootstrapping so the tenant row is
 * committed before child rows reference it via foreign key.
 */
@Service
@RequiredArgsConstructor
public class TenantCreationService {

    private final TenantRepository tenantRepository;

    @Transactional
    public Tenant persist(Tenant tenant) {
        return tenantRepository.save(tenant);
    }
}
