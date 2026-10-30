/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.profileimage.exception;

import com.emberone.profileimage.controller.ProfileImageController;
import com.emberone.profileimage.controller.SelfImageController;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handles profile-image domain errors for profile image endpoints only, so missing avatars map to
 * 204 and do not hit the global {@code ResourceNotFoundException} / {@code
 * IllegalArgumentException} handlers.
 */
@RestControllerAdvice(
        basePackageClasses = {ProfileImageController.class, SelfImageController.class})
@Order(0)
public class ProfileImageExceptionHandler {

    @ExceptionHandler(ProfileImageNotFoundException.class)
    public ResponseEntity<Void> handleProfileImageNotFound(ProfileImageNotFoundException ex) {
        // Expected for users without an avatar - 204, not 404 (not an error).
        return ResponseEntity.noContent().build();
    }
}
