/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.organizationgroup.dto.OrganizationGroupPageDto;
import com.emberone.organizationgroup.list.OrganizationGroupListFilter;
import com.emberone.organizationgroup.list.OrganizationGroupListStatus;
import com.emberone.organizationgroup.service.OrganizationGroupService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.userorganizationgroupmembership.service.UserOrganizationGroupMembershipService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class OrganizationGroupControllerTest {

    @Mock OrganizationGroupService organizationGroupService;
    @Mock UserOrganizationGroupMembershipService membershipService;

    @InjectMocks OrganizationGroupController controller;

    @Test
    void getGroupsByOrganizationDelegatesToService() {
        OrganizationGroupPageDto page =
                OrganizationGroupPageDto.builder().content(List.of()).page(0).size(50).build();
        ListQuery query = new ListQuery(0, null, null, null);
        OrganizationGroupListFilter filter =
                new OrganizationGroupListFilter(OrganizationGroupListStatus.ACTIVE);
        when(organizationGroupService.listVisibleGroupsByOrganizationId(15L, query, filter))
                .thenReturn(page);

        ResponseEntity<OrganizationGroupPageDto> response =
                controller.getGroupsByOrganization(15L, query, filter);

        assertThat(response.getBody()).isEqualTo(page);
    }
}
