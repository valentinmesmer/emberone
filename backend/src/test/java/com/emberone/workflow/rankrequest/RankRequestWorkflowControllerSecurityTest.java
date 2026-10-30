/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.rankrequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.security.SecurityService;
import com.emberone.platform.security.TokenService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.WorkflowDto;
import com.emberone.workflow.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RankRequestWorkflowController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class RankRequestWorkflowControllerSecurityTest {

    private static final String PAYLOAD = "{\"tenantRankId\":1}";

    @Autowired MockMvc mockMvc;

    @MockitoBean WorkflowService workflowService;

    @MockitoBean(name = "securityService")
    SecurityService workflowSecurityService;

    @Test
    void openRankRequestRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/workflows/rank-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(PAYLOAD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openRankRequestForbiddenWhenNotAllowed() throws Exception {
        when(workflowSecurityService.canBeginSelfServiceWorkflow(WorkflowType.RANK_REQUEST.getId()))
                .thenReturn(false);

        mockMvc.perform(
                        post("/api/v1/auth/workflows/rank-request")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(PAYLOAD))
                .andExpect(status().isForbidden());
    }

    @Test
    void openRankRequestAllowedWhenPermitted() throws Exception {
        when(workflowSecurityService.canBeginSelfServiceWorkflow(WorkflowType.RANK_REQUEST.getId()))
                .thenReturn(true);
        when(workflowService.open(
                        eq(WorkflowType.RANK_REQUEST.getId()),
                        any(),
                        eq("USER"),
                        eq(SecurityTestFixtures.USER_ID)))
                .thenReturn(WorkflowDto.builder().id(1L).build());

        mockMvc.perform(
                        post("/api/v1/auth/workflows/rank-request")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(PAYLOAD))
                .andExpect(status().isOk());
    }
}
