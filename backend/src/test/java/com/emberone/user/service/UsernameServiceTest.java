/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.admin.tenant.domain.Tenant;
import com.emberone.admin.tenant.repository.TenantRepository;
import com.emberone.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsernameServiceTest {

    private static final long TENANT_ID = 7L;

    @Mock private UserRepository userRepository;

    @Mock private TenantRepository tenantRepository;

    @InjectMocks private UsernameService usernameService;

    @Test
    void generateUniqueUsername_returnsBaseWhenAvailable() {
        stubTenant("fw");
        when(userRepository.existsByUsername("max.mustermann.fw")).thenReturn(false);

        assertThat(usernameService.generateUniqueUsername(TENANT_ID, "Max", "Mustermann"))
                .isEqualTo("max.mustermann.fw");
    }

    @Test
    void generateUniqueUsername_appendsNumericSuffixWhenBaseTaken() {
        stubTenant("fw");
        when(userRepository.existsByUsername("max.mustermann.fw")).thenReturn(true);
        when(userRepository.existsByUsername("max.mustermann2.fw")).thenReturn(false);

        assertThat(usernameService.generateUniqueUsername(TENANT_ID, "Max", "Mustermann"))
                .isEqualTo("max.mustermann2.fw");
    }

    @Test
    void generateUniqueUsername_normalizesUmlautsAndStripsInvalidCharacters() {
        stubTenant("acme");
        when(userRepository.existsByUsername("mueller.fuehrerschein.acme")).thenReturn(false);

        assertThat(usernameService.generateUniqueUsername(TENANT_ID, "Müller", "Führerschein"))
                .isEqualTo("mueller.fuehrerschein.acme");
    }

    private void stubTenant(String abbreviation) {
        when(tenantRepository.findById(TENANT_ID))
                .thenReturn(
                        Optional.of(
                                Tenant.builder().id(TENANT_ID).abbreviation(abbreviation).build()));
    }
}
