/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.organization.dto.OrganizationDto;
import com.emberone.organizationgroup.service.OrganizationGroupService;
import com.emberone.platform.security.TokenService;
import com.emberone.user.service.UserService;
import com.emberone.userorganizationmembership.dto.UserOrganizationMembershipDto;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class OrganizationSelfControllerTest {

    @Mock UserService userService;
    @Mock OrganizationGroupService organizationGroupService;
    @Mock TokenService tokenService;

    OrganizationSelfController controller;

    @BeforeEach
    void setUp() {
        controller =
                new OrganizationSelfController(userService, organizationGroupService, tokenService);
    }

    @Test
    void getMyOrganizationsDelegatesToUserService() {
        when(tokenService.requireUserId()).thenReturn(12L);
        List<UserOrganizationMembershipDto> memberships =
                List.of(UserOrganizationMembershipDto.builder().id(1L).build());
        when(userService.getOrganizationMembershipsForUser(12L)).thenReturn(memberships);

        ResponseEntity<List<UserOrganizationMembershipDto>> response =
                controller.getMyOrganizations();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(memberships);
    }

    @Test
    void getMyOrganizationDelegatesToUserService() {
        when(tokenService.requireUserId()).thenReturn(12L);
        OrganizationDto organization = OrganizationDto.builder().id(10L).name("FF Muster").build();
        when(userService.getOrganizationForMember(12L, 10L)).thenReturn(organization);

        ResponseEntity<OrganizationDto> response = controller.getMyOrganization(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(organization);
    }
}
