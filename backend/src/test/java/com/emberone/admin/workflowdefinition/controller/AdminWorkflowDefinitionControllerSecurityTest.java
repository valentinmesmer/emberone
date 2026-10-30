/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.workflowdefinition.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.workflow.dto.TenantWorkflowDefinitionDto;
import com.emberone.workflow.service.WorkflowDefinitionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminWorkflowDefinitionController.class)
@Import(SecurityWebMvcTestConfig.class)
class AdminWorkflowDefinitionControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean WorkflowDefinitionService definitionService;

    @Test
    void workflowDefinitionRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/admin/tenants/7/workflow-definitions/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void workflowDefinitionForbiddenForForeignTenant() throws Exception {
        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/8/workflow-definitions/1")
                                .with(SecurityTestFixtures.tenantAdminJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void workflowDefinitionAllowedForOwnTenant() throws Exception {
        when(definitionService.getTenantDefault(SecurityTestFixtures.TENANT_ID, 1L))
                .thenReturn(TenantWorkflowDefinitionDto.builder().workflowTypeId(1L).build());

        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/7/workflow-definitions/1")
                                .with(SecurityTestFixtures.tenantAdminJwt()))
                .andExpect(status().isOk());
    }
}
