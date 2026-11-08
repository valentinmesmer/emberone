/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.rankrequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.platform.ai.extraction.DocumentExtractionResult;
import com.emberone.platform.ai.extraction.DocumentExtractionService;
import com.emberone.platform.security.SecurityService;
import com.emberone.platform.security.TokenService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.WorkflowDto;
import com.emberone.workflow.service.WorkflowService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RankRequestWorkflowController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class RankRequestWorkflowControllerSecurityTest {

    private static final String PAYLOAD = "{\"tenantRankId\":1}";

    @Autowired MockMvc mockMvc;

    @MockitoBean WorkflowService workflowService;
    @MockitoBean DocumentExtractionService documentExtractionService;

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

    private static MockMultipartFile proof() {
        return new MockMultipartFile("file", "proof.jpg", "image/jpeg", new byte[] {1, 2, 3});
    }

    @Test
    void extractRequiresAuthentication() throws Exception {
        mockMvc.perform(multipart("/api/v1/auth/workflows/rank-request/extract").file(proof()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void extractForbiddenWithoutSamePermissionAsTheRequest() throws Exception {
        when(workflowSecurityService.canBeginSelfServiceWorkflow(WorkflowType.RANK_REQUEST.getId()))
                .thenReturn(false);

        mockMvc.perform(
                        multipart("/api/v1/auth/workflows/rank-request/extract")
                                .file(proof())
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());

        verify(documentExtractionService, never()).extract(any(), any(), any());
    }

    @Test
    void extractAllowedWhenRequestMayBeBegun() throws Exception {
        when(workflowSecurityService.canBeginSelfServiceWorkflow(WorkflowType.RANK_REQUEST.getId()))
                .thenReturn(true);
        when(documentExtractionService.extract(eq("RANK_REQUEST"), any(), isNull()))
                .thenReturn(new DocumentExtractionResult(Map.of(), List.of()));

        mockMvc.perform(
                        multipart("/api/v1/auth/workflows/rank-request/extract")
                                .file(proof())
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
