/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

import com.emberone.admin.tenant.domain.Tenant;
import com.emberone.admin.tenant.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminTenantPurgeBatchServiceTest {

    @Mock EntityManager entityManager;
    @Mock TenantRepository tenantRepository;
    @Mock Query nativeQuery;

    @InjectMocks AdminTenantPurgeBatchService purgeBatchService;

    @Test
    void deleteBatchSetsPurgingFlagAndUsesLimitedDelete() {
        Query purgeFlagQuery = mock(Query.class);
        when(entityManager.createNativeQuery("SELECT set_config('app.purging', 'true', true)"))
                .thenReturn(purgeFlagQuery);
        when(entityManager.createNativeQuery(contains("DELETE FROM action_log")))
                .thenReturn(nativeQuery);
        when(purgeFlagQuery.getSingleResult()).thenReturn("true");
        when(nativeQuery.setParameter("tenantId", 7L)).thenReturn(nativeQuery);
        when(nativeQuery.setParameter("limit", AdminTenantPurgeBatchService.BATCH_SIZE))
                .thenReturn(nativeQuery);
        when(nativeQuery.executeUpdate()).thenReturn(0);

        assertThat(purgeBatchService.deleteBatch(7L, "action_log")).isZero();
        verify(purgeFlagQuery).getSingleResult();
        verify(nativeQuery).executeUpdate();
    }

    @Test
    void deleteTenantSetsPurgingFlagAndRemovesTenant() {
        Tenant tenant = Tenant.builder().id(7L).name("Demo").abbreviation("demo").build();
        when(entityManager.createNativeQuery("SELECT set_config('app.purging', 'true', true)"))
                .thenReturn(nativeQuery);

        purgeBatchService.deleteTenant(tenant);

        verify(tenantRepository).delete(tenant);
        verify(entityManager).flush();
        verify(nativeQuery).getSingleResult();
    }
}
