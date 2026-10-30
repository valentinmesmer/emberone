/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.courseoffering.dto.CoursePrerequisiteCheckDto;
import com.emberone.courseoffering.dto.SelfCourseOfferingDto;
import com.emberone.courseoffering.dto.SelfCourseOfferingPageDto;
import com.emberone.courseoffering.list.SelfCourseOfferingListFilter;
import com.emberone.courseoffering.service.SelfCourseOfferingService;
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

@WebMvcTest(CourseOfferingSelfController.class)
@Import(SecurityWebMvcTestConfig.class)
class CourseOfferingSelfControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean SelfCourseOfferingService selfService;
    @MockitoBean TokenService tokenService;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/self/course-offerings")).andExpect(status().isUnauthorized());
    }

    @Test
    void listForbiddenForNonSelfServiceUser() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/self/course-offerings")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAllowedForSelfServiceUser() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(true);
        when(tokenService.requireUserId()).thenReturn(3L);
        when(selfService.list(eq(3L), any(), eq(new SelfCourseOfferingListFilter(false))))
                .thenReturn(SelfCourseOfferingPageDto.builder().content(List.of()).build());

        mockMvc.perform(
                        get("/api/v1/self/course-offerings")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void prerequisitesForbiddenForNonSelfServiceUser() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/self/course-offerings/5/prerequisites")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void prerequisitesAllowedForSelfServiceUser() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(true);
        when(tokenService.requireUserId()).thenReturn(3L);
        when(selfService.checkPrerequisites(3L, 5L))
                .thenReturn(CoursePrerequisiteCheckDto.builder().met(true).build());

        mockMvc.perform(
                        get("/api/v1/self/course-offerings/5/prerequisites")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void getForbiddenForNonSelfServiceUser() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(false);

        mockMvc.perform(
                        get("/api/v1/self/course-offerings/5")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllowedForSelfServiceUser() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(true);
        when(tokenService.requireUserId()).thenReturn(3L);
        when(selfService.get(3L, 5L)).thenReturn(SelfCourseOfferingDto.builder().id(5L).build());

        mockMvc.perform(
                        get("/api/v1/self/course-offerings/5")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void listPassesIncludeHeldFlag() throws Exception {
        when(coreSecurityService.isSelfServiceUser()).thenReturn(true);
        when(tokenService.requireUserId()).thenReturn(3L);
        when(selfService.list(eq(3L), any(), eq(new SelfCourseOfferingListFilter(true))))
                .thenReturn(SelfCourseOfferingPageDto.builder().content(List.of()).build());

        mockMvc.perform(
                        get("/api/v1/self/course-offerings?includeHeld=true")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }
}
