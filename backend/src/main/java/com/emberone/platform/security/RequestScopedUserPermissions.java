/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import com.emberone.userpermission.dto.UserPermissionDto;
import com.emberone.userpermission.service.UserPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Loads the current user's permission map at most once per HTTP request (Redis-backed loader still
 * applies across requests).
 */
@Component
@RequestScope
@RequiredArgsConstructor
public class RequestScopedUserPermissions {

    private final UserPermissionService userPermissionService;
    private final TokenService tokenService;

    private UserPermissionDto loaded;

    public UserPermissionDto getUnfiltered() {
        if (loaded != null) {
            return loaded;
        }
        Long userId = tokenService.getUserId();
        if (userId == null) {
            loaded = UserPermissionDto.builder().build();
        } else {
            loaded = userPermissionService.getUserPermissions(userId);
        }
        return loaded;
    }
}
