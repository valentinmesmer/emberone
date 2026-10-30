/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.emberone.identity.config.IdentityProviderProperties;
import com.emberone.identity.domain.IdentityUser;
import com.emberone.identity.provider.IdentityProviderUserProvisioner;
import com.emberone.platform.security.TenantContextHolder;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdentityDeprovisionServiceTest {

    @Mock IdentityDeprovisionTransactionService identityDeprovisionTransactionService;
    @Mock IdentityProviderUserProvisioner identityProviderUserProvisioner;

    IdentityDeprovisionService deprovisionService;

    @BeforeEach
    void setUp() {
        IdentityProviderProperties properties = new IdentityProviderProperties();
        deprovisionService =
                new IdentityDeprovisionService(
                        identityDeprovisionTransactionService,
                        identityProviderUserProvisioner,
                        properties);
        TenantContextHolder.setTenantId(7L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void processPendingDeprovisionCompletesIdentity() {
        IdentityUser identity = pendingIdentity();
        when(identityDeprovisionTransactionService.claimPendingDeprovisionIds(
                        any(), any(), anyInt()))
                .thenReturn(List.of(identity.getId()))
                .thenReturn(List.of());
        when(identityDeprovisionTransactionService.loadClaimedIdentityUser(eq(identity.getId())))
                .thenReturn(identity);

        deprovisionService.processPendingDeprovisions();

        verify(identityProviderUserProvisioner).deleteUser("remote-1");
        verify(identityDeprovisionTransactionService).completeDeprovision(identity.getId());
    }

    @Test
    void processPendingDeprovisionRetriesAfterIdpFailure() {
        IdentityUser identity = pendingIdentity();
        when(identityDeprovisionTransactionService.claimPendingDeprovisionIds(
                        any(), any(), anyInt()))
                .thenReturn(List.of(identity.getId()))
                .thenReturn(List.of());
        when(identityDeprovisionTransactionService.loadClaimedIdentityUser(eq(identity.getId())))
                .thenReturn(identity);
        doThrow(new RuntimeException("idp down"))
                .when(identityProviderUserProvisioner)
                .deleteUser("remote-1");

        deprovisionService.processPendingDeprovisions();

        assertThat(identity.getDeprovisionPendingAt()).isNotNull();
        assertThat(identity.getIdpUserId()).isEqualTo("remote-1");
        verify(identityDeprovisionTransactionService)
                .recordDeprovisionFailure(identity.getId(), "idp down");
        verify(identityDeprovisionTransactionService, never()).completeDeprovision(any());
    }

    private static IdentityUser pendingIdentity() {
        return IdentityUser.builder()
                .id(5L)
                .tenantId(7L)
                .emberoneUserId(1L)
                .idpUserId("remote-1")
                .displayName("Ada Lovelace")
                .givenName("Ada")
                .surname("Lovelace")
                .username("ada")
                .privateEmail("ada@example.com")
                .accountEnabled(false)
                .deprovisionPendingAt(Instant.now())
                .appRole("USER")
                .build();
    }
}
