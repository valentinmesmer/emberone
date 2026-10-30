/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.SecurityService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.workflow.service.WorkflowCommentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkflowCommentController.class)
@Import(SecurityWebMvcTestConfig.class)
class WorkflowCommentControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean WorkflowCommentService workflowCommentService;

    @MockitoBean(name = "securityService")
    SecurityService workflowSecurityService;

    @Test
    void workflowCommentsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/workflows/1/comments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void workflowCommentsForbiddenWhenCannotView() throws Exception {
        when(workflowSecurityService.canView(SecurityTestFixtures.WORKFLOW_ID)).thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/workflows/1/comments")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void workflowCommentsAllowedWhenCanView() throws Exception {
        when(workflowSecurityService.canView(SecurityTestFixtures.WORKFLOW_ID)).thenReturn(true);
        when(workflowCommentService.listComments(SecurityTestFixtures.WORKFLOW_ID))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/auth/workflows/1/comments")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
