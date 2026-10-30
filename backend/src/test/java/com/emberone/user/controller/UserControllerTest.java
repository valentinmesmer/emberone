/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.platform.security.TokenService;
import com.emberone.user.dto.CreateUserRequest;
import com.emberone.user.dto.UserDto;
import com.emberone.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock UserService userService;
    @Mock TokenService tokenService;

    @InjectMocks UserController controller;

    @Test
    void createUserForOrganizationReturnsCreated() {
        CreateUserRequest request = new CreateUserRequest();
        UserDto created = UserDto.builder().id(1L).build();
        when(tokenService.requireTenantId()).thenReturn(7L);
        when(userService.createInvitedUser(7L, 20L, request)).thenReturn(created);

        ResponseEntity<UserDto> response = controller.createUserForOrganization(20L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(created);
    }
}
