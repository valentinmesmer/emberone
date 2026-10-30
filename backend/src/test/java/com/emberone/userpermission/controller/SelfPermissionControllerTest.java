/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userpermission.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.platform.security.TokenService;
import com.emberone.userpermission.dto.UserPermissionDto;
import com.emberone.userpermission.service.UserPermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class SelfPermissionControllerTest {

    @Mock UserPermissionService userPermissionService;
    @Mock TokenService tokenService;

    SelfPermissionController controller;

    @BeforeEach
    void setUp() {
        controller = new SelfPermissionController(userPermissionService, tokenService);
    }

    @Test
    void getSelfPermissionsLoadsPermissionsWithoutOrganizationScope() {
        when(tokenService.requireUserId()).thenReturn(9L);
        UserPermissionDto permissions = UserPermissionDto.builder().build();
        when(userPermissionService.getUserPermissions(9L)).thenReturn(permissions);

        ResponseEntity<UserPermissionDto> response = controller.getSelfPermissions();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(permissions);
    }
}
