/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.organizationusermainorganizationchange;

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

@WebMvcTest(OrganizationUserMainOrganizationChangeWorkflowController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class OrganizationUserMainOrganizationChangeWorkflowControllerSecurityTest {

    private static final String BODY = "{\"newMainOrganizationId\":30,\"reason\":\"Move main\"}";

    @Autowired MockMvc mockMvc;

    @MockitoBean WorkflowService workflowService;

    @MockitoBean(name = "securityService")
    SecurityService workflowSecurityService;

    @Test
    void openRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/organizations/10/users/99/workflows/organization-user-main-organization-change")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openForbiddenWhenNotAllowed() throws Exception {
        when(workflowSecurityService.canOpenOrganizationUserMainOrganizationChangeWorkflow(
                        10L, 99L))
                .thenReturn(false);

        mockMvc.perform(
                        post("/api/v1/auth/organizations/10/users/99/workflows/organization-user-main-organization-change")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY)
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void openAllowedWhenPermitted() throws Exception {
        when(workflowSecurityService.canOpenOrganizationUserMainOrganizationChangeWorkflow(
                        10L, 99L))
                .thenReturn(true);
        when(workflowService.open(
                        eq(WorkflowType.ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE.getId()),
                        any(),
                        eq(
                                OrganizationUserMainOrganizationChangeWorkflowHandler
                                        .SUBJECT_ENTITY_TYPE),
                        eq(99L)))
                .thenReturn(WorkflowDto.builder().id(1L).build());

        mockMvc.perform(
                        post("/api/v1/auth/organizations/10/users/99/workflows/organization-user-main-organization-change")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY)
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
