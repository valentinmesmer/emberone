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
import com.emberone.platform.security.TokenService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.workflow.service.WorkflowDocumentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkflowDocumentController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class WorkflowDocumentControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean WorkflowDocumentService workflowDocumentService;

    @MockitoBean(name = "securityService")
    SecurityService documentSecurityService;

    @Test
    void workflowAttachmentsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/workflows/1/attachments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void workflowAttachmentsForbiddenWhenDenied() throws Exception {
        when(documentSecurityService.canDownloadWorkflowAttachment(
                        SecurityTestFixtures.WORKFLOW_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/auth/workflows/1/attachments")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void workflowAttachmentsAllowedWhenPermitted() throws Exception {
        when(documentSecurityService.canDownloadWorkflowAttachment(
                        SecurityTestFixtures.WORKFLOW_ID))
                .thenReturn(true);
        when(workflowDocumentService.listWorkflowAttachments(SecurityTestFixtures.WORKFLOW_ID))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/auth/workflows/1/attachments")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
