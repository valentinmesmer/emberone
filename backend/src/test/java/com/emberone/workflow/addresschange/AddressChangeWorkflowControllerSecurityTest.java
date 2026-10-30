/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.addresschange;

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

@WebMvcTest(AddressChangeWorkflowController.class)
@Import({SecurityWebMvcTestConfig.class, TokenService.class})
class AddressChangeWorkflowControllerSecurityTest {

    private static final String PAYLOAD =
            """
        {"street":"Main St","postalCode":"12345","city":"Berlin","country":"DE","attachmentIds":[1]}
        """;
    @Autowired MockMvc mockMvc;
    @MockitoBean WorkflowService workflowService;

    @MockitoBean(name = "securityService")
    SecurityService workflowSecurityService;

    @Test
    void openAddressChangeRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/workflows/address-change")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(PAYLOAD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openAddressChangeForbiddenWhenNotAllowed() throws Exception {
        when(workflowSecurityService.canBeginSelfServiceWorkflow(
                        WorkflowType.ADDRESS_CHANGE.getId()))
                .thenReturn(false);

        mockMvc.perform(
                        post("/api/v1/auth/workflows/address-change")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(PAYLOAD))
                .andExpect(status().isForbidden());
    }

    @Test
    void openAddressChangeAllowedWhenPermitted() throws Exception {
        when(workflowSecurityService.canBeginSelfServiceWorkflow(
                        WorkflowType.ADDRESS_CHANGE.getId()))
                .thenReturn(true);
        when(workflowService.open(
                        eq(WorkflowType.ADDRESS_CHANGE.getId()),
                        any(),
                        eq("USER"),
                        eq(SecurityTestFixtures.USER_ID)))
                .thenReturn(WorkflowDto.builder().id(1L).build());

        mockMvc.perform(
                        post("/api/v1/auth/workflows/address-change")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(PAYLOAD))
                .andExpect(status().isOk());
    }
}
