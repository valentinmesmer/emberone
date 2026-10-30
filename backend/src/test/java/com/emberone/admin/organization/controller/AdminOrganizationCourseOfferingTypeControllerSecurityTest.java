/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.organization.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.courseoffering.dto.CourseOfferingTypeSettingsDto;
import com.emberone.courseoffering.service.OrganizationCourseOfferingTypeService;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.testsupport.JwtTestSupport;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminOrganizationCourseOfferingTypeController.class)
@Import(SecurityWebMvcTestConfig.class)
class AdminOrganizationCourseOfferingTypeControllerSecurityTest {

    private static final String PATH =
            "/api/v1/auth/admin/tenants/7/organizations/10/course-offering-types";

    @Autowired MockMvc mockMvc;
    @MockitoBean OrganizationCourseOfferingTypeService courseOfferingTypeService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    void tenantAdminCanReadOnlyOwnTenant() throws Exception {
        when(courseOfferingTypeService.getSettings(7L, 10L))
                .thenReturn(
                        CourseOfferingTypeSettingsDto.builder()
                                .enabled(true)
                                .definitionConfigKeys(List.of())
                                .build());

        mockMvc.perform(
                        get(PATH)
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isOk());
        mockMvc.perform(
                        get("/api/v1/auth/admin/tenants/8/organizations/10/course-offering-types")
                                .with(
                                        JwtTestSupport.jwtForRole(
                                                EmberOneAppRole.TENANT_ADMIN, 7L, 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void organizationUserWithoutTenantAdminRoleIsForbidden() throws Exception {
        mockMvc.perform(get(PATH).with(JwtTestSupport.jwtForRole(EmberOneAppRole.USER, 7L, 1L)))
                .andExpect(status().isForbidden());
    }
}
