/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userpermission.controller;

import com.emberone.platform.security.TokenService;
import com.emberone.userpermission.dto.UserPermissionDto;
import com.emberone.userpermission.service.UserPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/self/permissions")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class SelfPermissionController {

    private final UserPermissionService userPermissionService;
    private final TokenService tokenService;

    @GetMapping
    public ResponseEntity<UserPermissionDto> getSelfPermissions() {
        UserPermissionDto permissions =
                userPermissionService.getUserPermissions(tokenService.requireUserId());
        return ResponseEntity.ok(permissions);
    }
}
