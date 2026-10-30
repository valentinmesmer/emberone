/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.absence.dto.CreateUserAbsenceRequest;
import com.emberone.absence.dto.UserAbsenceDto;
import com.emberone.absence.dto.UserAbsencePageDto;
import com.emberone.absence.list.AbsenceListFilter;
import com.emberone.absence.list.AbsenceSelfListFilterOptions;
import com.emberone.absence.service.UserAbsenceService;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class UserAbsenceSelfControllerTest {

    @Mock UserAbsenceService userAbsenceService;
    @Mock TokenService tokenService;
    @Mock AbsenceSelfListFilterOptions selfListFilterOptions;

    UserAbsenceSelfController controller;

    @BeforeEach
    void setUp() {
        controller =
                new UserAbsenceSelfController(
                        userAbsenceService, tokenService, selfListFilterOptions);
    }

    @Test
    void createAbsenceReturnsCreatedForAuthenticatedUser() {
        when(tokenService.requireUserId()).thenReturn(5L);
        CreateUserAbsenceRequest request = new CreateUserAbsenceRequest();
        UserAbsenceDto created = UserAbsenceDto.builder().id(1L).build();
        when(userAbsenceService.createAbsence(5L, request)).thenReturn(created);

        ResponseEntity<UserAbsenceDto> response = controller.createAbsence(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(created);
        verify(userAbsenceService).createAbsence(5L, request);
    }

    @Test
    void listAbsencesDelegatesToService() {
        when(tokenService.requireUserId()).thenReturn(5L);
        UserAbsencePageDto page =
                UserAbsencePageDto.builder()
                        .content(List.of(UserAbsenceDto.builder().id(2L).build()))
                        .build();
        ListQuery query = new ListQuery(0, null, null, null);
        AbsenceListFilter filter = AbsenceListFilter.empty();
        when(userAbsenceService.listAbsencesForUser(5L, query, filter)).thenReturn(page);

        ResponseEntity<UserAbsencePageDto> response =
                controller.listAbsences(query, null, null, null, List.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(page);
    }
}
