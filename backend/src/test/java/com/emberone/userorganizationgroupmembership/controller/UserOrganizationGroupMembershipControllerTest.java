/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationgroupmembership.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.userorganizationgroupmembership.dto.CreateUserOrganizationGroupMembershipRequest;
import com.emberone.userorganizationgroupmembership.dto.UserOrganizationGroupMembershipDto;
import com.emberone.userorganizationgroupmembership.service.UserOrganizationGroupMembershipService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class UserOrganizationGroupMembershipControllerTest {

    @Mock UserOrganizationGroupMembershipService membershipService;

    @InjectMocks UserOrganizationGroupMembershipController controller;

    @Test
    void createMembershipReturnsCreated() {
        CreateUserOrganizationGroupMembershipRequest request =
                new CreateUserOrganizationGroupMembershipRequest();
        UserOrganizationGroupMembershipDto created =
                UserOrganizationGroupMembershipDto.builder().id(1L).build();
        when(membershipService.createMembership(request)).thenReturn(created);

        ResponseEntity<UserOrganizationGroupMembershipDto> response =
                controller.createMembership(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(created);
    }
}
