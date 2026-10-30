/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.emberone.admin.tenant.domain.Tenant;
import com.emberone.admin.tenant.dto.PurgeTenantRequest;
import com.emberone.admin.tenant.repository.TenantRepository;
import com.emberone.identity.service.IdentityUserService;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.storage.service.ObjectStorageBucketCatalog;
import com.emberone.platform.storage.service.ObjectStorageDeletionOutboxService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AdminTenantPurgeServiceTest {

    @Mock TenantRepository tenantRepository;
    @Mock ObjectStorageBucketCatalog objectStorageBucketCatalog;
    @Mock ObjectStorageDeletionOutboxService objectStorageDeletionOutboxService;
    @Mock TokenService tokenService;
    @Mock IdentityUserService identityUserService;
    @Mock AdminTenantPurgeTransactionService purgeTransactionService;

    AdminTenantPurgeService purgeService;

    @BeforeEach
    void setUp() {
        purgeService =
                new AdminTenantPurgeService(
                        tenantRepository,
                        objectStorageBucketCatalog,
                        objectStorageDeletionOutboxService,
                        tokenService,
                        identityUserService,
                        purgeTransactionService);
        TenantContextHolder.setTenantId(7L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void purgeRejectsTenantScopeMismatch() {
        PurgeTenantRequest request = new PurgeTenantRequest();
        request.setConfirmationName("Demo");

        assertThatThrownBy(() -> purgeService.purgeTenant(8L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Tenant scope mismatch");
        verifyNoInteractions(tenantRepository, objectStorageDeletionOutboxService);
    }

    @Test
    void purgeRejectsMismatchedConfirmation() {
        Tenant tenant = Tenant.builder().id(7L).name("Demo").abbreviation("demo").build();
        when(tenantRepository.findById(7L)).thenReturn(Optional.of(tenant));

        PurgeTenantRequest request = new PurgeTenantRequest();
        request.setConfirmationName("Other");

        assertThatThrownBy(() -> purgeService.purgeTenant(7L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Confirmation");
    }

    @Test
    void purgeEnqueuesAllBucketsAndDeletesTenant() {
        Tenant tenant = Tenant.builder().id(7L).name("Demo").abbreviation("demo").build();
        when(tenantRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(tokenService.getUserId()).thenReturn(99L);
        when(objectStorageBucketCatalog.tenantScopedBuckets())
                .thenReturn(List.of("docs", "profiles", "workflows", "attachments"));

        PurgeTenantRequest request = new PurgeTenantRequest();
        request.setConfirmationName("Demo");

        purgeService.purgeTenant(7L, request);

        verify(objectStorageDeletionOutboxService).enqueuePrefixDeletion(7L, "docs", "tenants/7/");
        verify(objectStorageDeletionOutboxService)
                .enqueuePrefixDeletion(7L, "profiles", "tenants/7/");
        verify(objectStorageDeletionOutboxService)
                .enqueuePrefixDeletion(7L, "workflows", "tenants/7/");
        verify(objectStorageDeletionOutboxService)
                .enqueuePrefixDeletion(7L, "attachments", "tenants/7/");
        verify(identityUserService).deprovisionAllIdpUsersForTenantPurge(7L);
        verify(purgeTransactionService).purgeTenantData(7L);
        verify(purgeTransactionService).deleteTenant(tenant);
        verify(tenantRepository, never()).delete(any(Tenant.class));
    }
}
