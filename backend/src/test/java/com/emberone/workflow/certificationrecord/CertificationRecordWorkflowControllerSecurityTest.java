/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.certificationrecord;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(CertificationRecordWorkflowController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class CertificationRecordWorkflowControllerSecurityTest {

    private static final String PAYLOAD =
            """
            {"tenantCertDefinitionId":1,"completedDate":"2026-01-15","attachmentIds":[2]}
            """;

    @Autowired MockMvc mockMvc;

    @MockitoBean WorkflowService workflowService;
    @MockitoBean DocumentExtractionService documentExtractionService;

    @MockitoBean(name = "securityService")
    SecurityService workflowSecurityService;

    @Test
    void openCertificationRecordRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/workflows/certification-record")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(PAYLOAD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openCertificationRecordForbiddenWhenNotAllowed() throws Exception {
        when(workflowSecurityService.canBeginSelfServiceWorkflow(
                        WorkflowType.CERTIFICATION_RECORD.getId()))
                .thenReturn(false);

        mockMvc.perform(
                        post("/api/v1/auth/workflows/certification-record")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(PAYLOAD))
                .andExpect(status().isForbidden());
    }

    @Test
    void openCertificationRecordAllowedWhenPermitted() throws Exception {
        when(workflowSecurityService.canBeginSelfServiceWorkflow(
                        WorkflowType.CERTIFICATION_RECORD.getId()))
                .thenReturn(true);
        when(workflowService.open(
                        eq(WorkflowType.CERTIFICATION_RECORD.getId()),
                        any(),
                        eq("USER"),
                        eq(SecurityTestFixtures.USER_ID)))
                .thenReturn(WorkflowDto.builder().id(1L).build());

        mockMvc.perform(
                        post("/api/v1/auth/workflows/certification-record")
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
        mockMvc.perform(
                        multipart("/api/v1/auth/workflows/certification-record/extract")
                                .file(proof()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void extractForbiddenWithoutSamePermissionAsTheRequest() throws Exception {
        when(workflowSecurityService.canBeginSelfServiceWorkflow(
                        WorkflowType.CERTIFICATION_RECORD.getId()))
                .thenReturn(false);

        mockMvc.perform(
                        multipart("/api/v1/auth/workflows/certification-record/extract")
                                .file(proof())
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());

        verify(documentExtractionService, never()).extract(any(), any(), any());
    }

    @Test
    void extractAllowedWhenRequestMayBeBegun() throws Exception {
        when(workflowSecurityService.canBeginSelfServiceWorkflow(
                        WorkflowType.CERTIFICATION_RECORD.getId()))
                .thenReturn(true);
        when(documentExtractionService.extract(eq("CERTIFICATION_RECORD"), any(), eq(5L)))
                .thenReturn(new DocumentExtractionResult(Map.of(), List.of()));

        mockMvc.perform(
                        multipart("/api/v1/auth/workflows/certification-record/extract")
                                .file(proof())
                                .param("tenantCertDefinitionId", "5")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
