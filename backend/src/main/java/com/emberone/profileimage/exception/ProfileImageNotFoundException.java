/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.profileimage.exception;

import com.emberone.platform.exception.ResourceNotFoundException;

/**
 * Thrown when a user has no profile image. Expected for many users; maps to HTTP 204 without noisy
 * logging.
 */
public class ProfileImageNotFoundException extends ResourceNotFoundException {

    public ProfileImageNotFoundException(Long userId) {
        super("Profile image not found for user: " + userId);
    }
}
