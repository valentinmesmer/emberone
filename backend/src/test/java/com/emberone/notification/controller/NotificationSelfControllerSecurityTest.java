/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.notification.dto.NotificationDto;
import com.emberone.notification.dto.NotificationPageDto;
import com.emberone.notification.dto.UnreadNotificationCountDto;
import com.emberone.notification.list.NotificationSelfListFilterOptions;
import com.emberone.notification.service.NotificationSelfService;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationSelfController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class NotificationSelfControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean NotificationSelfService notificationSelfService;

    @MockitoBean NotificationSelfListFilterOptions selfListFilterOptions;

    @Test
    void listFilterOptionsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/notifications/list-filter-options"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listFilterOptionsAllowedForTenantUser() throws Exception {
        when(selfListFilterOptions.filterOptions())
                .thenReturn(new ListFilterOptionsDto(java.util.Map.of()));

        mockMvc.perform(
                        get("/api/v1/self/notifications/list-filter-options")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void selfNotificationsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/notifications")).andExpect(status().isUnauthorized());
    }

    @Test
    void selfNotificationsAllowedForTenantUser() throws Exception {
        when(notificationSelfService.listNotifications(any(), any()))
                .thenReturn(NotificationPageDto.builder().content(List.of()).build());

        mockMvc.perform(
                        get("/api/v1/self/notifications")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());

        verify(notificationSelfService).listNotifications(any(), any());
    }

    @Test
    void listUsesAuthenticatedUserContextOnly() throws Exception {
        NotificationDto own =
                NotificationDto.builder().id(1L).type("WORKFLOW_APPROVED").unread(true).build();
        when(notificationSelfService.listNotifications(any(), any()))
                .thenReturn(
                        NotificationPageDto.builder()
                                .content(List.of(own))
                                .totalElements(1)
                                .build());

        mockMvc.perform(
                        get("/api/v1/self/notifications")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));

        verify(notificationSelfService).listNotifications(any(), any());
    }

    @Test
    void unreadCountAllowedForTenantUser() throws Exception {
        when(notificationSelfService.getUnreadCount())
                .thenReturn(UnreadNotificationCountDto.builder().count(2).build());

        mockMvc.perform(
                        get("/api/v1/self/notifications/unread-count")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void markReadAllowedForTenantUser() throws Exception {
        when(notificationSelfService.markRead(9L))
                .thenReturn(NotificationDto.builder().id(9L).unread(false).build());

        mockMvc.perform(
                        post("/api/v1/self/notifications/9/read")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void markAllReadAllowedForTenantUser() throws Exception {
        mockMvc.perform(
                        post("/api/v1/self/notifications/read-all")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isNoContent());

        verify(notificationSelfService).markAllRead();
    }
}
