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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emberone.courseoffering.dto.CourseAudienceOptionsDto;
import com.emberone.courseoffering.dto.CourseOfferingDto;
import com.emberone.courseoffering.dto.CourseOfferingPageDto;
import com.emberone.courseoffering.list.CourseOfferingOrganizationListFilterOptions;
import com.emberone.courseoffering.service.CourseAudienceOptionsService;
import com.emberone.courseoffering.service.CourseOfferingCancellationService;
import com.emberone.courseoffering.service.CourseOfferingCatalogService;
import com.emberone.courseoffering.service.CourseOfferingService;
import com.emberone.courseoffering.service.CourseOfferingTemplateService;
import com.emberone.platform.security.SecurityService;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.testsupport.SecurityTestFixtures;
import com.emberone.testsupport.SecurityWebMvcTestConfig;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CourseOfferingController.class)
@Import(SecurityWebMvcTestConfig.class)
class CourseOfferingControllerSecurityTest {

    private static final String BASE = "/api/v1/auth/organizations/10/course-offerings";
    private static final String CREATE_BODY =
            "{\"trainingCourseTypeId\":1,\"topic\":\"Atemschutz\",\"maxParticipants\":10}";

    private static final String CANCEL_BODY = "{\"reason\":\"Zu wenige\"}";

    private static final String UPDATE_WITH_QUOTAS_BODY =
            "{\"topic\":\"A\",\"maxParticipants\":4,"
                    + "\"audienceQuotas\":[{\"audienceId\":1,\"quota\":4}]}";

    @Autowired MockMvc mockMvc;

    @MockitoBean CourseOfferingService offeringService;
    @MockitoBean CourseOfferingCatalogService catalogService;
    @MockitoBean CourseAudienceOptionsService audienceOptionsService;
    @MockitoBean CourseOfferingTemplateService templateService;
    @MockitoBean CourseOfferingCancellationService cancellationService;
    @MockitoBean CourseOfferingOrganizationListFilterOptions organizationListFilterOptions;

    @MockitoBean(name = "securityService")
    SecurityService coreSecurityService;

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    void listForbiddenWithoutRead() throws Exception {
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(get(BASE).with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAllowedWithRead() throws Exception {
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(offeringService.list(eq(SecurityTestFixtures.ORG_ID), any(), any()))
                .thenReturn(CourseOfferingPageDto.builder().content(List.of()).build());

        mockMvc.perform(get(BASE).with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void listFilterOptionsForbiddenWithoutRead() throws Exception {
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        get(BASE + "/list-filter-options")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listFilterOptionsAllowedWithRead() throws Exception {
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(organizationListFilterOptions.forOrganization(SecurityTestFixtures.ORG_ID))
                .thenReturn(new ListFilterOptionsDto(Map.of()));

        mockMvc.perform(
                        get(BASE + "/list-filter-options")
                                .with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void listTemplatesForbiddenWithoutRead() throws Exception {
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(get(BASE + "/templates").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listTemplatesAllowedWithRead() throws Exception {
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(offeringService.listTemplates(eq(SecurityTestFixtures.ORG_ID), any()))
                .thenReturn(CourseOfferingPageDto.builder().content(List.of()).build());

        mockMvc.perform(get(BASE + "/templates").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void createForbiddenWithReadOnly() throws Exception {
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(coreSecurityService.hasCourseOfferingWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        post(BASE)
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAllowedWithWrite() throws Exception {
        when(coreSecurityService.hasCourseOfferingWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(offeringService.create(eq(SecurityTestFixtures.ORG_ID), any()))
                .thenReturn(CourseOfferingDto.builder().id(1L).build());

        mockMvc.perform(
                        post(BASE)
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CREATE_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void publishForbiddenWithoutWrite() throws Exception {
        when(coreSecurityService.hasCourseOfferingWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(post(BASE + "/5/publish").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        post(BASE + "/5/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CANCEL_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancelForbiddenWithReadOnly() throws Exception {
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(coreSecurityService.hasCourseOfferingWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        post(BASE + "/5/cancel")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CANCEL_BODY))
                .andExpect(status().isForbidden());
        verify(cancellationService, never()).cancel(any(), any(), any());
    }

    @Test
    void cancelAllowedWithWrite() throws Exception {
        when(coreSecurityService.hasCourseOfferingWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(cancellationService.cancel(eq(SecurityTestFixtures.ORG_ID), eq(5L), eq("Zu wenige")))
                .thenReturn(CourseOfferingDto.builder().id(5L).build());

        mockMvc.perform(
                        post(BASE + "/5/cancel")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CANCEL_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void cancelRequiresAReason() throws Exception {
        when(coreSecurityService.hasCourseOfferingWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);

        mockMvc.perform(
                        post(BASE + "/5/cancel")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        post(BASE + "/5/cancel")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
        verify(cancellationService, never()).cancel(any(), any(), any());
    }

    @Test
    void prerequisitesForbiddenWithoutWrite() throws Exception {
        when(coreSecurityService.hasCourseOfferingWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        put(BASE + "/5/prerequisites")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"trainingCourseRequirementMode\":\"ALL\","
                                                + "\"certRequirementMode\":\"ALL\","
                                                + "\"drivingLicenseRequirementMode\":\"ALL\","
                                                + "\"entries\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void templateActionsForbiddenWithoutWrite() throws Exception {
        when(coreSecurityService.hasCourseOfferingWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        post(BASE + "/5/save-as-template")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"templateName\":\"Vorlage\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        post(BASE + "/5/instantiate")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"firstSessionDate\":\"2027-06-01\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void audiencesForbiddenWithoutWrite() throws Exception {
        when(coreSecurityService.hasCourseOfferingWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        put(BASE + "/5/audiences")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"entries\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void offerableTypesForbiddenWithoutWrite() throws Exception {
        when(coreSecurityService.hasCourseOfferingWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(get(BASE + "/offerable-types").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void audienceOptionsForbiddenWithoutRead() throws Exception {
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(get(BASE + "/audience-options").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void audienceOptionsAllowedWithRead() throws Exception {
        when(coreSecurityService.hasCourseOfferingReadPermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(audienceOptionsService.listForOrganization(SecurityTestFixtures.ORG_ID))
                .thenReturn(CourseAudienceOptionsDto.builder().build());

        mockMvc.perform(get(BASE + "/audience-options").with(SecurityTestFixtures.tenantUserJwt()))
                .andExpect(status().isOk());
    }

    @Test
    void quotaChangesNeedWritePermission() throws Exception {
        when(coreSecurityService.hasCourseOfferingWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(false);

        mockMvc.perform(
                        put(BASE + "/5/audiences")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"entries\":[{\"kind\":\"ORG\",\"refId\":3,\"quota\":4}]}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        put(BASE + "/5")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(UPDATE_WITH_QUOTAS_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void quotaMustBePositive() throws Exception {
        when(coreSecurityService.hasCourseOfferingWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);

        mockMvc.perform(
                        put(BASE + "/5/audiences")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"entries\":[{\"kind\":\"ORG\",\"refId\":3,\"quota\":0}]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        put(BASE + "/5")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"topic\":\"A\",\"maxParticipants\":4,"
                                                + "\"audienceQuotas\":[{\"audienceId\":1,\"quota\":0}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quotasAreAcceptedWithWritePermission() throws Exception {
        when(coreSecurityService.hasCourseOfferingWritePermission(SecurityTestFixtures.ORG_ID))
                .thenReturn(true);
        when(offeringService.replaceAudiences(eq(SecurityTestFixtures.ORG_ID), eq(5L), any()))
                .thenReturn(CourseOfferingDto.builder().id(5L).build());
        when(offeringService.update(eq(SecurityTestFixtures.ORG_ID), eq(5L), any()))
                .thenReturn(CourseOfferingDto.builder().id(5L).build());

        mockMvc.perform(
                        put(BASE + "/5/audiences")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"entries\":[{\"kind\":\"ORG\",\"refId\":3,\"quota\":4}]}"))
                .andExpect(status().isOk());
        mockMvc.perform(
                        put(BASE + "/5")
                                .with(SecurityTestFixtures.tenantUserJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(UPDATE_WITH_QUOTAS_BODY))
                .andExpect(status().isOk());
    }
}
