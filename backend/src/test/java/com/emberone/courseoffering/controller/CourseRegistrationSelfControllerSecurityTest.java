/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.courseoffering.dto.SelfCourseRegistrationDto;
import com.emberone.courseoffering.dto.SelfCourseRegistrationPageDto;
import com.emberone.courseoffering.service.CourseRegistrationQueryService;
import com.emberone.courseoffering.service.CourseRegistrationService;
import com.emberone.platform.security.SecurityService;
import com.emberone.platform.security.TokenService;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** The caller's own registrations: self-service only, always scoped to the caller. */
@WebMvcTest(CourseRegistrationSelfController.class)
@Import(SecurityWebMvcTestConfig.class)
class CourseRegistrationSelfControllerSecurityTest {

    private static final String URL = "/api/v1/self/course-registrations";

    @Autowired MockMvc mockMvc;

    @MockitoBean CourseRegistrationQueryService queryService;
    @MockitoBean CourseRegistrationService registrationService;
    @MockitoBean TokenService tokenService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void listForbiddenForNonSelfServiceUser() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(false);

        mockMvc.perform(get(URL).with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
        verify(queryService, never()).listForSelf(any(), any());
    }

    @Test
    void listIsScopedToTheCaller() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(true);
        when(tokenService.requireUserId()).thenReturn(3L);
        when(queryService.listForSelf(eq(3L), any()))
                .thenReturn(SelfCourseRegistrationPageDto.builder().content(List.of()).build());

        mockMvc.perform(get(URL).with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());

        verify(queryService).listForSelf(eq(3L), any());
    }

    @Test
    void getForbiddenForNonSelfServiceUser() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(false);

        mockMvc.perform(get(URL + "/5").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
        verify(queryService, never()).getForSelf(any(), any());
    }

    @Test
    void getIsScopedToTheCaller() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(true);
        when(tokenService.requireUserId()).thenReturn(3L);
        when(queryService.getForSelf(3L, 5L))
                .thenReturn(SelfCourseRegistrationDto.builder().id(5L).build());

        mockMvc.perform(get(URL + "/5").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());

        verify(queryService).getForSelf(3L, 5L);
    }

    @Test
    void withdrawRequiresAuthentication() throws Exception {
        mockMvc.perform(post(URL + "/5/withdraw")).andExpect(status().isUnauthorized());
    }

    @Test
    void withdrawForbiddenForNonSelfServiceUser() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(false);

        mockMvc.perform(post(URL + "/5/withdraw").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
        verify(registrationService, never()).withdrawFromWaitlist(any(), any());
    }

    @Test
    void withdrawUsesTheCallerAsOwner() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(true);
        when(tokenService.requireTenantId()).thenReturn(7L);
        when(tokenService.requireUserId()).thenReturn(3L);

        mockMvc.perform(post(URL + "/5/withdraw").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isNoContent());

        verify(registrationService).withdrawFromWaitlist(3L, 5L);
    }
}
