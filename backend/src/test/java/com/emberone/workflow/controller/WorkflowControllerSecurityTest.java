/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.security.SecurityService;
import com.emberone.testsupport.JwtTestSupport;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.workflow.dto.WorkflowDto;
import com.emberone.workflow.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkflowController.class)
@Import(SecurityWebMvcTestConfig.class)
class WorkflowControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean WorkflowService workflowService;

    @MockitoBean(name = "securityService")
    SecurityService workflowSecurityService;

    @Test
    void workflowDetailRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/workflows/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void workflowDetailForbiddenWhenSecurityServiceDenies() throws Exception {
        when(workflowSecurityService.canView(1L)).thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/workflows/1")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 42L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void workflowDetailAllowedWhenSecurityServicePermits() throws Exception {
        when(workflowSecurityService.canView(1L)).thenReturn(true);
        when(workflowService.getWorkflow(1L)).thenReturn(WorkflowDto.builder().id(1L).build());

        mockMvc.perform(
                        get("/api/v1/auth/workflows/1")
                                .with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 42L)))
                .andExpect(status().isOk());
    }
}
