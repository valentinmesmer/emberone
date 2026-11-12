/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.admin.workflowdefinition.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import com.emberone.workflow.dto.TenantWorkflowDefinitionDto;
import com.emberone.workflow.dto.WorkflowRoutingRuleDto;
import com.emberone.workflow.service.WorkflowDefinitionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminWorkflowDefinitionController.class)
@Import(SecurityWebMvcTestConfig.class)
class AdminWorkflowDefinitionControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean WorkflowDefinitionService definitionService;

    @Test
    void workflowDefinitionListRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/admin/tenants/7/workflow-definitions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void workflowDefinitionListForbiddenForForeignTenant() throws Exception {
        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/8/workflow-definitions")
                                .with(SecurityTestFixtures.tenantAdminJwt()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(definitionService);
    }

    @Test
    void workflowDefinitionListAllowedForOwnTenant() throws Exception {
        when(definitionService.listTenantDefaults(SecurityTestFixtures.TENANT_ID))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/7/workflow-definitions")
                                .with(SecurityTestFixtures.tenantAdminJwt()))
                .andExpect(status().isOk());
    }

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

    private static final String RULE_BODY = "{\"subjectIds\":[5]}";

    @Test
    void routingRuleEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/admin/tenants/7/workflow-definitions/3/routing-rules")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(RULE_BODY))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                        put("/api/v1/auth/admin/tenants/7/workflow-definitions/3/routing-rules/9")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(RULE_BODY))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                        delete(
                                "/api/v1/auth/admin/tenants/7/workflow-definitions/3/routing-rules/9"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/admin/tenants/7/workflow-definitions/3/routing-rules/9"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void routingRuleEndpointsForbiddenForForeignTenant() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/admin/tenants/8/workflow-definitions/3/routing-rules")
                                .with(SecurityTestFixtures.tenantAdminJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(RULE_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        put("/api/v1/auth/admin/tenants/8/workflow-definitions/3/routing-rules/9")
                                .with(SecurityTestFixtures.tenantAdminJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(RULE_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        delete(
                                        "/api/v1/auth/admin/tenants/8/workflow-definitions/3/routing-rules/9")
                                .with(SecurityTestFixtures.tenantAdminJwt()))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/8/workflow-definitions/3/routing-rules/9")
                                .with(SecurityTestFixtures.tenantAdminJwt()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(definitionService);
    }

    @Test
    void routingRuleEndpointsAllowedForOwnTenant() throws Exception {
        WorkflowRoutingRuleDto rule =
                WorkflowRoutingRuleDto.builder().id(9L).subjectIds(List.of(5L)).build();
        when(definitionService.getRoutingRule(SecurityTestFixtures.TENANT_ID, 3L, 9L))
                .thenReturn(rule);
        when(definitionService.createRoutingRule(eq(SecurityTestFixtures.TENANT_ID), eq(3L), any()))
                .thenReturn(rule);
        when(definitionService.updateRoutingRule(
                        eq(SecurityTestFixtures.TENANT_ID), eq(3L), eq(9L), any()))
                .thenReturn(rule);

        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/7/workflow-definitions/3/routing-rules/9")
                                .with(SecurityTestFixtures.tenantAdminJwt()))
                .andExpect(status().isOk());
        mockMvc.perform(
                        post("/api/v1/auth/admin/tenants/7/workflow-definitions/3/routing-rules")
                                .with(SecurityTestFixtures.tenantAdminJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(RULE_BODY))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        put("/api/v1/auth/admin/tenants/7/workflow-definitions/3/routing-rules/9")
                                .with(SecurityTestFixtures.tenantAdminJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(RULE_BODY))
                .andExpect(status().isOk());
        mockMvc.perform(
                        delete(
                                        "/api/v1/auth/admin/tenants/7/workflow-definitions/3/routing-rules/9")
                                .with(SecurityTestFixtures.tenantAdminJwt()))
                .andExpect(status().isNoContent());
    }
}
