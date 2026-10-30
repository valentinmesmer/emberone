/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.profileimage.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ProfileImageExceptionHandlerTest {

    private final ProfileImageExceptionHandler handler = new ProfileImageExceptionHandler();

    @Test
    void missingProfileImageReturnsNoContent() {
        ResponseEntity<Void> response =
                handler.handleProfileImageNotFound(new ProfileImageNotFoundException(42L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }
}
