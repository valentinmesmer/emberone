/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.admin.tenant.repository.TenantRepository;
import com.emberone.management.domain.ManagementActivation;
import com.emberone.management.list.ManagementActivationListFilter;
import com.emberone.management.repository.ManagementActivationRepository;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ManagementActivationLogServiceTest {

    private static final ListQuery QUERY = new ListQuery(0, null, null, null);

    @Mock ManagementActivationRepository activationRepository;
    @Mock TenantRepository tenantRepository;
    @Mock TokenService tokenService;

    @InjectMocks ManagementActivationLogService service;

    @SuppressWarnings("unchecked")
    private void stubEmptyPage() {
        Page<ManagementActivation> empty = new PageImpl<>(List.of());
        when(activationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(empty);
    }

    @Test
    void organizationUserIsRejected() {
        assertThatThrownBy(() -> service.listForAdmin(QUERY, ManagementActivationListFilter.NONE))
                .isInstanceOf(ResponseStatusException.class);
        verify(activationRepository, never())
                .findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void tenantAdminCannotFilterOtherTenants() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())).thenReturn(true);
        when(tokenService.getTenantId()).thenReturn(7L);
        ManagementActivationListFilter other =
                ManagementActivationListFilter.parse(List.of(8L), null, null, null);

        assertThatThrownBy(() -> service.listForAdmin(QUERY, other))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("outside your tenant");
        verify(activationRepository, never())
                .findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void tenantAdminWithoutTenantContextIsRejected() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())).thenReturn(true);
        when(tokenService.getTenantId()).thenReturn(null);

        assertThatThrownBy(() -> service.listForAdmin(QUERY, ManagementActivationListFilter.NONE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("tenant");
    }

    @Test
    void tenantAdminListsOwnTenant() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())).thenReturn(true);
        when(tokenService.getTenantId()).thenReturn(7L);
        stubEmptyPage();

        assertThat(service.listForAdmin(QUERY, ManagementActivationListFilter.NONE).getContent())
                .isEmpty();
    }

    @Test
    void globalAdminMayFilterAnyTenant() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(true);
        stubEmptyPage();

        assertThat(
                        service.listForAdmin(
                                        QUERY,
                                        ManagementActivationListFilter.parse(
                                                List.of(8L), null, null, null))
                                .getContent())
                .isEmpty();
    }
}
