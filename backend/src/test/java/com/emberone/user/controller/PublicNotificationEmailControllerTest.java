/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.emberone.platform.security.TenantContextHolder;
import com.emberone.user.dto.PublicVerifyNotificationEmailRequest;
import com.emberone.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class PublicNotificationEmailControllerTest {

    @Mock UserService userService;

    PublicNotificationEmailController controller;

    @BeforeEach
    void setUp() {
        controller = new PublicNotificationEmailController(userService);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void verifyBindsTenantAndDelegatesToService() {
        PublicVerifyNotificationEmailRequest request =
                PublicVerifyNotificationEmailRequest.builder()
                        .token("raw-token")
                        .tenantId(7L)
                        .build();

        ResponseEntity<Void> response = controller.verifyNotificationEmail(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(7L);
        verify(userService).verifyNotificationEmailByToken("raw-token");
    }
}
