/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.controller;

import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.dto.NotificationPreferenceDto;
import com.emberone.notification.dto.NotificationPreferencesDto;
import com.emberone.notification.dto.UpdateNotificationPreferenceRequest;
import com.emberone.notification.service.UserNotificationPreferenceService;
import com.emberone.platform.security.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/self/notification-preferences")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class NotificationPreferenceSelfController {

    private final UserNotificationPreferenceService preferenceService;
    private final TokenService tokenService;

    @GetMapping
    public ResponseEntity<NotificationPreferencesDto> getPreferences() {
        return ResponseEntity.ok(preferenceService.getPreferences(tokenService.requireUserId()));
    }

    @PutMapping("/{type}")
    public ResponseEntity<NotificationPreferenceDto> updatePreference(
            @PathVariable NotificationType type,
            @Valid @RequestBody UpdateNotificationPreferenceRequest request) {
        return ResponseEntity.ok(
                preferenceService.updatePreference(tokenService.requireUserId(), type, request));
    }
}
