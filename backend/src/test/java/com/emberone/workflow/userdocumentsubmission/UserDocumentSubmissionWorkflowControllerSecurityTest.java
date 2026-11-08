/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.userdocumentsubmission;

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

@WebMvcTest(UserDocumentSubmissionWorkflowController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class UserDocumentSubmissionWorkflowControllerSecurityTest {

    private static final String PAYLOAD =
            """
            {"title":"Test document","attachmentIds":[1]}
            """;

    @Autowired MockMvc mockMvc;

    @MockitoBean WorkflowService workflowService;
    @MockitoBean DocumentExtractionService documentExtractionService;

    @MockitoBean(name = "securityService")
    SecurityService workflowSecurityService;

    @Test
    void openRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/workflows/user-document-submission")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(PAYLOAD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openForbiddenWhenNotAllowed() throws Exception {
        when(workflowSecurityService.canBeginSelfServiceWorkflow(
                        WorkflowType.USER_DOCUMENT_SUBMISSION.getId()))
                .thenReturn(false);

        mockMvc.perform(
                        post("/api/v1/auth/workflows/user-document-submission")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(PAYLOAD))
                .andExpect(status().isForbidden());
    }

    @Test
    void openAllowedWhenPermitted() throws Exception {
        when(workflowSecurityService.canBeginSelfServiceWorkflow(
                        WorkflowType.USER_DOCUMENT_SUBMISSION.getId()))
                .thenReturn(true);
        when(workflowService.open(
                        eq(WorkflowType.USER_DOCUMENT_SUBMISSION.getId()),
                        any(),
                        eq("USER"),
                        eq(SecurityTestFixtures.USER_ID)))
                .thenReturn(WorkflowDto.builder().id(1L).build());

        mockMvc.perform(
                        post("/api/v1/auth/workflows/user-document-submission")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(PAYLOAD))
                .andExpect(status().isOk());
    }

    private static MockMultipartFile document() {
        return new MockMultipartFile("file", "doc.jpg", "image/jpeg", new byte[] {1, 2, 3});
    }

    @Test
    void extractRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        multipart("/api/v1/auth/workflows/user-document-submission/extract")
                                .file(document()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void extractForbiddenWithoutSamePermissionAsTheRequest() throws Exception {
        when(workflowSecurityService.canBeginSelfServiceWorkflow(
                        WorkflowType.USER_DOCUMENT_SUBMISSION.getId()))
                .thenReturn(false);

        mockMvc.perform(
                        multipart("/api/v1/auth/workflows/user-document-submission/extract")
                                .file(document())
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());

        verify(documentExtractionService, never()).extract(any(), any(), any());
    }

    @Test
    void extractAllowedWhenRequestMayBeBegun() throws Exception {
        when(workflowSecurityService.canBeginSelfServiceWorkflow(
                        WorkflowType.USER_DOCUMENT_SUBMISSION.getId()))
                .thenReturn(true);
        when(documentExtractionService.extract(eq("USER_DOCUMENT_SUBMISSION"), any(), isNull()))
                .thenReturn(new DocumentExtractionResult(Map.of(), List.of()));

        mockMvc.perform(
                        multipart("/api/v1/auth/workflows/user-document-submission/extract")
                                .file(document())
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
