/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.SecurityService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.workflow.dto.WorkflowPageDto;
import com.emberone.workflow.list.WorkflowSelfListFilterOptions;
import com.emberone.workflow.service.WorkflowService;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkflowSelfController.class)
@Import(SecurityWebMvcTestConfig.class)
class WorkflowSelfControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean WorkflowService workflowService;
    @MockitoBean WorkflowHandlerRegistry handlerRegistry;
    @MockitoBean WorkflowSelfListFilterOptions selfListFilterOptions;

    @MockitoBean(name = "securityService")
    SecurityService workflowSecurityService;

    @Test
    void openedRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/workflows/opened")).andExpect(status().isUnauthorized());
    }

    @Test
    void openedAllowedForTenantUser() throws Exception {
        when(workflowSecurityService.isSelfServiceUser()).thenReturn(true);
        when(workflowService.listOpenedForCurrentUser(any(), any()))
                .thenReturn(
                        WorkflowPageDto.builder()
                                .content(List.of())
                                .page(0)
                                .size(50)
                                .totalElements(0)
                                .totalPages(0)
                                .hasNext(false)
                                .build());

        mockMvc.perform(
                        get("/api/v1/self/workflows/opened")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
