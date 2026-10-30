/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.timeentry.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.platform.security.TokenService;
import com.emberone.timeentry.dto.CreateUserTimeEntryRequest;
import com.emberone.timeentry.dto.UserTimeEntryDto;
import com.emberone.timeentry.list.TimeEntrySelfListFilterOptions;
import com.emberone.timeentry.service.UserTimeEntryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class UserTimeEntrySelfControllerTest {

    @Mock UserTimeEntryService userTimeEntryService;
    @Mock TokenService tokenService;
    @Mock TimeEntrySelfListFilterOptions selfListFilterOptions;

    UserTimeEntrySelfController controller;

    @BeforeEach
    void setUp() {
        controller =
                new UserTimeEntrySelfController(
                        userTimeEntryService, tokenService, selfListFilterOptions);
    }

    @Test
    void createTimeEntryReturnsCreatedForAuthenticatedUser() {
        when(tokenService.requireUserId()).thenReturn(6L);
        CreateUserTimeEntryRequest request = new CreateUserTimeEntryRequest();
        UserTimeEntryDto created = UserTimeEntryDto.builder().id(1L).build();
        when(userTimeEntryService.createTimeEntry(6L, request)).thenReturn(created);

        ResponseEntity<UserTimeEntryDto> response = controller.createTimeEntry(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(created);
    }
}
