/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
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

    @GetMapping("/organizations/{organizationId}/users/{userId}")
    @PreAuthorize("@securityService.hasUserReadPermission(#organizationId)")
    public ResponseEntity<UserDto> getUserForOrganization(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserForOrganization(organizationId, userId));
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
