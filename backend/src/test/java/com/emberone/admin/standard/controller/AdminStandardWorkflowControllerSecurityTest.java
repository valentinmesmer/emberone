/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.admin.standard.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.admin.standard.dto.StandardContentDto;
import com.emberone.admin.standard.dto.StandardWorkflowDto;
import com.emberone.admin.standard.dto.StandardWorkflowSlotDto;
import com.emberone.admin.standard.service.AdminStandardService;
import com.emberone.platform.exception.ValidationException;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Security and request validation of {@code GET/PUT …/standards/{id}/workflows/{configKey}}. */
@WebMvcTest(AdminStandardController.class)
@Import(SecurityWebMvcTestConfig.class)
class AdminStandardWorkflowControllerSecurityTest {

    private static final String BODY =
            """
            {"enabled":true,"slots":[
              {"slotRole":"CONTEXT","approverOrgGroupConfigKey":"LEADERSHIP",
               "autoApprove":false,"selfApproveAllowed":true},
              {"slotRole":"ADDITIONAL","approverOrgGroupConfigKey":"LEADERSHIP",
               "autoApprove":true,"selfApproveAllowed":false}]}
            """;

    @Autowired MockMvc mockMvc;

    @MockitoBean AdminStandardService adminStandardService;

    @Test
    void workflowEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/admin/tenants/7/standards/1/workflows/4"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                        put("/api/v1/auth/admin/tenants/7/standards/1/workflows/4")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(adminStandardService);
    }

    @Test
    void regularUserCannotReadOrUpdateWorkflows() throws Exception {
        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/7/standards/1/workflows/4")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        put("/api/v1/auth/admin/tenants/7/standards/1/workflows/4")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(adminStandardService);
    }

    @Test
    void workflowEndpointsForbiddenForForeignTenant() throws Exception {
        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/8/standards/1/workflows/4")
                                .with(SecurityTestFixtures.tenantAdminJwt()))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        put("/api/v1/auth/admin/tenants/8/standards/1/workflows/4")
                                .with(SecurityTestFixtures.tenantAdminJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(adminStandardService);
    }

    @Test
    void tenantAdminReadsTheWorkflowWithAllSlots() throws Exception {
        when(adminStandardService.getWorkflow(7L, 1L, "4"))
                .thenReturn(
                        StandardWorkflowDto.builder()
                                .configKey("4")
                                .workflowTypeId(4L)
                                .enabled(true)
                                .slots(
                                        List.of(
                                                StandardWorkflowSlotDto.builder()
                                                        .slotRole("CONTEXT")
                                                        .approverOrgGroupConfigKey("LEADERSHIP")
                                                        .selfApproveAllowed(true)
                                                        .build(),
                                                StandardWorkflowSlotDto.builder()
                                                        .slotRole("ADDITIONAL")
                                                        .approverOrgGroupConfigKey("LEADERSHIP")
                                                        .autoApprove(true)
                                                        .build()))
                                .build());

        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/7/standards/1/workflows/4")
                                .with(SecurityTestFixtures.tenantAdminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configKey").value("4"))
                .andExpect(jsonPath("$.workflowTypeId").value(4))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.slots[0].slotRole").value("CONTEXT"))
                .andExpect(jsonPath("$.slots[0].approverOrgGroupConfigKey").value("LEADERSHIP"))
                .andExpect(jsonPath("$.slots[0].selfApproveAllowed").value(true))
                .andExpect(jsonPath("$.slots[1].slotRole").value("ADDITIONAL"))
                .andExpect(jsonPath("$.slots[1].autoApprove").value(true));
    }

    @Test
    void tenantAdminUpdatesTheWorkflowPerSlot() throws Exception {
        when(adminStandardService.updateWorkflow(eq(7L), eq(1L), eq("4"), any()))
                .thenReturn(StandardContentDto.builder().build());

        mockMvc.perform(
                        put("/api/v1/auth/admin/tenants/7/standards/1/workflows/4")
                                .with(SecurityTestFixtures.tenantAdminJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY))
                .andExpect(status().isOk());
    }

    @Test
    void updateRejectsRequestWithoutSlotsOrWithBlankFields() throws Exception {
        mockMvc.perform(
                        put("/api/v1/auth/admin/tenants/7/standards/1/workflows/4")
                                .with(SecurityTestFixtures.tenantAdminJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"enabled\":true}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        put("/api/v1/auth/admin/tenants/7/standards/1/workflows/4")
                                .with(SecurityTestFixtures.tenantAdminJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"enabled\":true,\"slots\":[{\"slotRole\":\"\","
                                                + "\"approverOrgGroupConfigKey\":\"LEADERSHIP\"}]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(adminStandardService);
    }

    @Test
    void updateWithUnknownOrMissingRoleIsABadRequest() throws Exception {
        when(adminStandardService.updateWorkflow(eq(7L), eq(1L), eq("4"), any()))
                .thenThrow(new ValidationException("Unknown approver slot role 'BOGUS'"));

        mockMvc.perform(
                        put("/api/v1/auth/admin/tenants/7/standards/1/workflows/4")
                                .with(SecurityTestFixtures.tenantAdminJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY))
                .andExpect(status().isBadRequest());
    }
}
