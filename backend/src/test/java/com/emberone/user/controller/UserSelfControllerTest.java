/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.event.dto.ServiceHoursTotalDto;
import com.emberone.event.service.EventAttendanceService;
import com.emberone.platform.security.TokenService;
import com.emberone.user.dto.SelfMembershipHistoryDto;
import com.emberone.user.dto.UpdateContactInformationRequest;
import com.emberone.user.dto.UserDto;
import com.emberone.user.service.SelfMembershipHistoryService;
import com.emberone.user.service.ServiceHoursTotalService;
import com.emberone.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class UserSelfControllerTest {

    @Mock UserService userService;
    @Mock EventAttendanceService eventAttendanceService;
    @Mock ServiceHoursTotalService serviceHoursTotalService;
    @Mock SelfMembershipHistoryService selfMembershipHistoryService;
    @Mock TokenService tokenService;

    UserSelfController controller;

    @BeforeEach
    void setUp() {
        controller =
                new UserSelfController(
                        userService,
                        eventAttendanceService,
                        serviceHoursTotalService,
                        selfMembershipHistoryService,
                        tokenService);
    }

    @Test
    void getMembershipHistoryDelegatesToService() {
        SelfMembershipHistoryDto history =
                SelfMembershipHistoryDto.builder()
                        .organizations(List.of())
                        .groups(List.of())
                        .build();
        when(tokenService.requireUserId()).thenReturn(1L);
        when(selfMembershipHistoryService.getForUser(1L)).thenReturn(history);

        assertThat(controller.getMembershipHistory().getBody()).isSameAs(history);
    }

    @Test
    void getUserDataDelegatesToService() {
        when(tokenService.requireUserId()).thenReturn(4L);
        UserDto user = UserDto.builder().id(4L).build();
        when(userService.getUserById(4L)).thenReturn(user);

        ResponseEntity<UserDto> response = controller.getUserData();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(user);
    }

    @Test
    void getServiceHoursTotalPassesYearToHoursService() {
        when(tokenService.requireUserId()).thenReturn(4L);
        ServiceHoursTotalDto total = ServiceHoursTotalDto.builder().totalHours(10.0).build();
        when(serviceHoursTotalService.getForUser(4L, 2025)).thenReturn(total);

        ResponseEntity<ServiceHoursTotalDto> response = controller.getServiceHoursTotal(2025);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(total);
    }

    @Test
    void updateNotificationEmailDelegatesToService() {
        when(tokenService.requireUserId()).thenReturn(4L);
        UpdateContactInformationRequest request = new UpdateContactInformationRequest();
        request.setPrivateEmail("ada@example.com");
        UserDto updated = UserDto.builder().id(4L).build();
        when(userService.updateContactInformationSelf(4L, request)).thenReturn(updated);

        ResponseEntity<UserDto> response = controller.updateNotificationEmail(request);

        assertThat(response.getBody()).isEqualTo(updated);
    }
}
