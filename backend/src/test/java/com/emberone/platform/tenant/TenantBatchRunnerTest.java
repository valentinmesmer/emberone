/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.admin.tenant.repository.TenantRepository;
import com.emberone.platform.security.TenantContextHolder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class TenantBatchRunnerTest {

    @Mock TenantRepository tenantRepository;
    @InjectMocks TenantBatchRunner runner;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        MDC.clear();
    }

    @Test
    void setsAndClearsTenantContextForEachTenant() {
        when(tenantRepository.findAllIdsOrderByIdAsc()).thenReturn(List.of(1L, 2L));

        List<Long> tenantIdsDuringAction = new ArrayList<>();
        List<Long> contextTenantIdsDuringAction = new ArrayList<>();
        List<String> mdcTenantIdsDuringAction = new ArrayList<>();
        List<String> mdcRequestIdsDuringAction = new ArrayList<>();
        runner.forEachTenant(
                tenantId -> {
                    tenantIdsDuringAction.add(tenantId);
                    contextTenantIdsDuringAction.add(TenantContextHolder.getTenantId());
                    mdcTenantIdsDuringAction.add(MDC.get("tenantId"));
                    mdcRequestIdsDuringAction.add(MDC.get("requestId"));
                });

        assertThat(tenantIdsDuringAction).containsExactly(1L, 2L);
        assertThat(contextTenantIdsDuringAction).containsExactly(1L, 2L);
        assertThat(mdcTenantIdsDuringAction).containsExactly("1", "2");
        assertThat(mdcRequestIdsDuringAction).hasSize(2);
        assertThat(mdcRequestIdsDuringAction.get(0)).isNotEqualTo(mdcRequestIdsDuringAction.get(1));
        assertThat(mdcRequestIdsDuringAction)
                .allMatch(
                        id ->
                                id.matches(
                                        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        assertThat(TenantContextHolder.getTenantId()).isNull();
        assertThat(MDC.get("tenantId")).isNull();
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void continuesAfterTenantFailure() {
        when(tenantRepository.findAllIdsOrderByIdAsc()).thenReturn(List.of(1L, 2L, 3L));

        List<Long> processed = new ArrayList<>();
        runner.forEachTenant(
                tenantId -> {
                    if (tenantId == 2L) {
                        throw new IllegalStateException("tenant 2 failed");
                    }
                    processed.add(tenantId);
                });

        assertThat(processed).containsExactly(1L, 3L);
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void clearsContextWhenActionFails() {
        when(tenantRepository.findAllIdsOrderByIdAsc()).thenReturn(List.of(5L));

        runner.forEachTenant(
                tenantId -> {
                    throw new RuntimeException("boom");
                });

        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void doesNothingWhenNoTenantsExist() {
        when(tenantRepository.findAllIdsOrderByIdAsc()).thenReturn(List.of());

        runner.forEachTenant(
                tenantId -> {
                    throw new AssertionError("should not run");
                });

        assertThat(TenantContextHolder.getTenantId()).isNull();
    }
}
