/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.admin.tenant.dto.CreateTenantRequest;
import com.emberone.admin.tenant.dto.TenantDto;
import com.emberone.admin.tenant.service.AdminTenantPurgeService;
import com.emberone.admin.tenant.service.AdminTenantService;
import com.emberone.federalstate.domain.FederalState;
import com.emberone.platform.web.PageDto;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminTenantControllerTest {

    @Mock AdminTenantService tenantAdminService;
    @Mock AdminTenantPurgeService adminTenantPurgeService;

    @InjectMocks AdminTenantController controller;

    @Test
    void listTenantsDelegatesToService() {
        PageDto<TenantDto> page =
                PageDto.<TenantDto>builder()
                        .content(List.of(TenantDto.builder().id(1L).build()))
                        .build();
        when(tenantAdminService.listTenants(any(), any())).thenReturn(page);

        ResponseEntity<PageDto<TenantDto>> response = controller.listTenants(null, null);

        assertThat(response.getBody()).isEqualTo(page);
    }

    @Test
    void listTenantsParsesFederalStateFilter() {
        when(tenantAdminService.listTenants(any(), any()))
                .thenReturn(PageDto.<TenantDto>builder().build());

        controller.listTenants(null, List.of("BAYERN", "BERLIN"));

        verify(tenantAdminService)
                .listTenants(
                        any(),
                        argThat(
                                filter ->
                                        filter.federalStates()
                                                .equals(
                                                        List.of(
                                                                FederalState.BAYERN,
                                                                FederalState.BERLIN))));
    }

    @Test
    void createTenantReturnsCreated() {
        CreateTenantRequest request = new CreateTenantRequest();
        TenantDto created = TenantDto.builder().id(2L).build();
        when(tenantAdminService.createTenant(request)).thenReturn(created);

        ResponseEntity<TenantDto> response = controller.createTenant(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(created);
    }
}
