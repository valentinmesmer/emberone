/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.workflowtypecatalog.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminWorkflowTypeCatalogController.class)
@Import(SecurityWebMvcTestConfig.class)
class AdminWorkflowTypeCatalogControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean WorkflowHandlerRegistry handlerRegistry;

    @Test
    void workflowTypeCatalogRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/admin/tenants/7/workflow-types"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void workflowTypeCatalogForbiddenForForeignTenant() throws Exception {
        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/8/workflow-types")
                                .with(SecurityTestFixtures.tenantAdminJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void workflowTypeCatalogAllowedForOwnTenant() throws Exception {
        when(handlerRegistry.all()).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/7/workflow-types")
                                .with(SecurityTestFixtures.tenantAdminJwt()))
                .andExpect(status().isOk());
    }
}
