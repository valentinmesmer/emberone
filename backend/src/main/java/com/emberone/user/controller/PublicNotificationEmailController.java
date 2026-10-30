/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.controller;

import com.emberone.platform.security.TenantScopeGuard;
import com.emberone.user.dto.PublicVerifyNotificationEmailRequest;
import com.emberone.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicNotificationEmailController {

    private final UserService userService;

    @PostMapping("/notification-email/verify")
    public ResponseEntity<Void> verifyNotificationEmail(
            @Valid @RequestBody PublicVerifyNotificationEmailRequest request) {
        TenantScopeGuard.bind(request.getTenantId());
        userService.verifyNotificationEmailByToken(request.getToken());
        return ResponseEntity.noContent().build();
    }
}
