/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.dutyfunction.service.DutyFunctionResolutionService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.rank.dto.OrganizationRankMemberPageDto;
import com.emberone.rank.service.TenantRankCatalogService;
import com.emberone.rank.service.UserRankService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class OrganizationRankControllerTest {

    private static final long ORG_ID = 42L;

    @Mock UserRankService userRankService;
    @Mock TenantRankCatalogService tenantRankCatalogService;
    @Mock DutyFunctionResolutionService dutyFunctionResolutionService;

    OrganizationRankController controller;

    @BeforeEach
    void setUp() {
        controller =
                new OrganizationRankController(
                        userRankService, tenantRankCatalogService, dutyFunctionResolutionService);
    }

    @Test
    void listMembersDelegatesToService() {
        ListQuery query = new ListQuery(0, 50, "muster", "lastName,asc");
        OrganizationRankMemberPageDto page =
                OrganizationRankMemberPageDto.builder().content(List.of()).totalElements(0).build();
        when(userRankService.listOrganizationMembers(ORG_ID, query)).thenReturn(page);

        ResponseEntity<OrganizationRankMemberPageDto> response =
                controller.listMembers(ORG_ID, query);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(page);
        verify(userRankService).listOrganizationMembers(ORG_ID, query);
    }
}
