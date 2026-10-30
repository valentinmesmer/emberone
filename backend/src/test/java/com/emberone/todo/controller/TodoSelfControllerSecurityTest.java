/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.todo.dto.TodoDto;
import com.emberone.todo.dto.TodoPageDto;
import com.emberone.todo.list.TodoSelfListFilterOptions;
import com.emberone.todo.service.TodoSelfService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TodoSelfController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class TodoSelfControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean TodoSelfService todoSelfService;

    @MockitoBean TodoSelfListFilterOptions selfListFilterOptions;

    @Test
    void listFilterOptionsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/todos/list-filter-options"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listFilterOptionsAllowedForTenantUser() throws Exception {
        when(selfListFilterOptions.filterOptions()).thenReturn(new ListFilterOptionsDto(Map.of()));

        mockMvc.perform(
                        get("/api/v1/self/todos/list-filter-options")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void selfTodosRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/todos")).andExpect(status().isUnauthorized());
    }

    @Test
    void selfTodosAllowedForTenantUser() throws Exception {
        when(todoSelfService.listTodos(any(), any()))
                .thenReturn(TodoPageDto.builder().content(List.of()).openCount(0).build());

        mockMvc.perform(get("/api/v1/self/todos").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());

        verify(todoSelfService).listTodos(any(), any());
    }

    @Test
    void listUsesAuthenticatedUserContextOnly() throws Exception {
        TodoDto own =
                TodoDto.builder().id(1L).type("WORKFLOW_APPROVAL_REQUIRED").open(true).build();
        when(todoSelfService.listTodos(any(), any()))
                .thenReturn(
                        TodoPageDto.builder()
                                .content(List.of(own))
                                .openCount(1)
                                .totalElements(1)
                                .build());

        mockMvc.perform(get("/api/v1/self/todos").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.openCount").value(1));
    }

    @Test
    void getTodoRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/todos/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void getTodoAllowedForTenantUser() throws Exception {
        when(todoSelfService.getTodo(1L)).thenReturn(TodoDto.builder().id(1L).open(true).build());

        mockMvc.perform(get("/api/v1/self/todos/1").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(todoSelfService).getTodo(1L);
    }

    @Test
    void completeAllowedForTenantUser() throws Exception {
        when(todoSelfService.complete(3L)).thenReturn(TodoDto.builder().id(3L).open(false).build());

        mockMvc.perform(
                        post("/api/v1/self/todos/3/complete")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());

        verify(todoSelfService).complete(3L);
    }

    @Test
    void openCountAllowedForTenantUser() throws Exception {
        when(todoSelfService.countOpenTodos()).thenReturn(2L);

        mockMvc.perform(
                        get("/api/v1/self/todos/open-count")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }
}
