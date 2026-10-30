/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.notification.domain.NotificationChannel;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.dto.NotificationPreferenceDto;
import com.emberone.notification.dto.NotificationPreferencesDto;
import com.emberone.notification.service.UserNotificationPreferenceService;
import com.emberone.platform.security.TokenService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationPreferenceSelfController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class NotificationPreferenceSelfControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserNotificationPreferenceService preferenceService;

    @Test
    void preferencesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/notification-preferences"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPreferencesAllowedForTenantUser() throws Exception {
        when(preferenceService.getPreferences(SecurityTestFixtures.USER_ID))
                .thenReturn(
                        NotificationPreferencesDto.builder()
                                .preferences(
                                        List.of(
                                                NotificationPreferenceDto.builder()
                                                        .type(NotificationType.TODO_ASSIGNED)
                                                        .channels(
                                                                Set.of(NotificationChannel.IN_APP))
                                                        .defaultChannels(
                                                                Set.of(
                                                                        NotificationChannel.IN_APP,
                                                                        NotificationChannel.EMAIL))
                                                        .customized(true)
                                                        .build()))
                                .build());

        mockMvc.perform(
                        get("/api/v1/self/notification-preferences")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferences[0].type").value("TODO_ASSIGNED"));

        verify(preferenceService).getPreferences(SecurityTestFixtures.USER_ID);
    }

    @Test
    void updatePreferenceAllowedForTenantUser() throws Exception {
        when(preferenceService.updatePreference(
                        eq(SecurityTestFixtures.USER_ID),
                        eq(NotificationType.WORKFLOW_APPROVED),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(
                        NotificationPreferenceDto.builder()
                                .type(NotificationType.WORKFLOW_APPROVED)
                                .channels(EnumSet.of(NotificationChannel.EMAIL))
                                .defaultChannels(
                                        EnumSet.of(
                                                NotificationChannel.IN_APP,
                                                NotificationChannel.EMAIL))
                                .customized(true)
                                .build());

        mockMvc.perform(
                        put("/api/v1/self/notification-preferences/WORKFLOW_APPROVED")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"channels\":[\"EMAIL\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("WORKFLOW_APPROVED"));
    }
}
