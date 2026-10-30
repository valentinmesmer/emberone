/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.controller;

import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.user.dto.CreateUserRequest;
import com.emberone.user.dto.UpdateUserDataRequest;
import com.emberone.user.dto.UserDto;
import com.emberone.user.dto.UserPageDto;
import com.emberone.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TokenService tokenService;

    @GetMapping("/organizations/{organizationId}")
    @PreAuthorize("@securityService.hasUserReadPermission(#organizationId)")
    public ResponseEntity<UserPageDto> getUsersForOrganization(
            @PathVariable Long organizationId, ListQuery query) {
        return ResponseEntity.ok(userService.listUsersForOrganization(organizationId, query));
    }

    @PostMapping("/organizations/{organizationId}")
    @PreAuthorize("@securityService.hasUserWritePermission(#organizationId)")
    public ResponseEntity<UserDto> createUserForOrganization(
            @PathVariable Long organizationId, @Valid @RequestBody CreateUserRequest request) {
        UserDto created =
                userService.createInvitedUser(
                        tokenService.requireTenantId(), organizationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/organizations/{organizationId}/users/{userId}/personal-data")
    @PreAuthorize("@securityService.hasUserWritePermission(#organizationId)")
    public ResponseEntity<UserDto> updateUserPersonalData(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserDataRequest request) {
        UserDto updated = userService.updateUserData(userId, organizationId, request);
        return ResponseEntity.ok(updated);
    }
}
