/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.service;

import com.emberone.admin.tenant.domain.Tenant;
import com.emberone.admin.tenant.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** One purge batch per transaction so Spring AOP applies (no self-invocation). */
@Service
@RequiredArgsConstructor
public class AdminTenantPurgeBatchService {

    static final int BATCH_SIZE = 5000;

    private final EntityManager entityManager;
    private final TenantRepository tenantRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteBatch(long tenantId, String table) {
        enablePurgingSessionFlag();
        // Explicit tenant filter: purge deletes by target tenant id, also on tables without RLS.
        String sql =
                """
                DELETE FROM %s
                WHERE ctid IN (
                    SELECT ctid FROM %s WHERE tenant_id = :tenantId LIMIT :limit
                )
                """
                        .formatted(table, table);
        return entityManager
                .createNativeQuery(sql)
                .setParameter("tenantId", tenantId)
                .setParameter("limit", BATCH_SIZE)
                .executeUpdate();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteTenant(Tenant tenant) {
        enablePurgingSessionFlag();
        tenantRepository.delete(tenant);
        entityManager.flush();
    }

    private void enablePurgingSessionFlag() {
        entityManager
                .createNativeQuery("SELECT set_config('app.purging', 'true', true)")
                .getSingleResult();
    }
}
