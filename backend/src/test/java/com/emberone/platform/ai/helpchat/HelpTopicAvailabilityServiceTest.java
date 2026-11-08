/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai.helpchat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.organization.dto.OrganizationDto;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.security.SecurityService;
import com.emberone.platform.security.TokenService;
import com.emberone.user.service.UserService;
import com.emberone.userorganizationmembership.dto.UserOrganizationMembershipDto;
import com.emberone.userpermission.dto.UserPermissionDto;
import com.emberone.userpermission.service.UserPermissionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HelpTopicAvailabilityServiceTest {

    @Mock TokenService tokenService;
    @Mock SecurityService securityService;
    @Mock UserPermissionService userPermissionService;
    @Mock UserService userService;

    HelpTopicAvailabilityService service;

    @BeforeEach
    void setUp() {
        HelpTextProvider texts =
                new HelpTextProvider(
                        Map.of(),
                        Map.of(
                                "MY_EMERGENCY_CONTACTS", "text",
                                "ORG_SERVICE_EVENTS", "text",
                                "ORG_OPERATION_EVENTS", "text",
                                "ADMIN_USERS", "text",
                                "ADMIN_ORGANIZATIONS", "text"));
        service =
                new HelpTopicAvailabilityService(
                        tokenService, securityService, userPermissionService, userService, texts);
        when(tokenService.requireUserId()).thenReturn(5L);
    }

    private static UserOrganizationMembershipDto membership(long id, String name) {
        OrganizationDto organization = new OrganizationDto();
        organization.setId(id);
        organization.setName(name);
        UserOrganizationMembershipDto membership = new UserOrganizationMembershipDto();
        membership.setOrganization(organization);
        return membership;
    }

    private void permissions(Map<Long, List<PermissionType>> permissions) {
        when(userPermissionService.getUserPermissions(5L))
                .thenReturn(UserPermissionDto.builder().permissions(permissions).build());
    }

    @Test
    void organizationTopicsFollowPermissionsPerOrganization() {
        permissions(
                Map.of(
                        1L, List.of(PermissionType.SERVICE_EVENT_READ),
                        2L,
                                List.of(
                                        PermissionType.SERVICE_EVENT_READ,
                                        PermissionType.SERVICE_EVENT_WRITE)));
        when(userService.getOrganizationMembershipsForUser(5L))
                .thenReturn(List.of(membership(1L, "FF Bdorf"), membership(2L, "FF Ahausen")));

        Map<HelpTopic, AvailableHelpTopic> topics = service.availableForCurrentUser();

        AvailableHelpTopic services = topics.get(HelpTopic.ORG_SERVICE_EVENTS);
        assertThat(services.readOrganizations())
                .extracting(HelpChatOrganizationDto::name)
                .containsExactly("FF Ahausen", "FF Bdorf");
        assertThat(services.createOrganizations())
                .extracting(HelpChatOrganizationDto::id)
                .containsExactly(2L);
        assertThat(services.canCreate()).isTrue();
        // No OPERATION_EVENT_READ anywhere → topic not offered.
        assertThat(topics).doesNotContainKey(HelpTopic.ORG_OPERATION_EVENTS);
        // Self topic with help text is always there; self topics without help text are not.
        assertThat(topics).containsKey(HelpTopic.MY_EMERGENCY_CONTACTS);
        assertThat(topics).doesNotContainKey(HelpTopic.MY_ABSENCES);
    }

    @Test
    void permissionsWithoutMembershipNameAreIgnored() {
        permissions(Map.of(9L, List.of(PermissionType.SERVICE_EVENT_READ)));
        when(userService.getOrganizationMembershipsForUser(5L)).thenReturn(List.of());

        assertThat(service.availableForCurrentUser())
                .doesNotContainKey(HelpTopic.ORG_SERVICE_EVENTS);
    }

    @Test
    void adminTopicsOnlyForAdmins() {
        permissions(Map.of());
        when(userService.getOrganizationMembershipsForUser(5L)).thenReturn(List.of());
        when(securityService.isTenantAdmin()).thenReturn(false);
        when(securityService.isGlobalAdmin()).thenReturn(false);

        assertThat(service.availableForCurrentUser())
                .doesNotContainKeys(HelpTopic.ADMIN_USERS, HelpTopic.ADMIN_ORGANIZATIONS);
    }

    @Test
    void tenantScopedAdminTopicsNeedTenantFromToken() {
        permissions(Map.of());
        when(userService.getOrganizationMembershipsForUser(5L)).thenReturn(List.of());
        when(securityService.isTenantAdmin()).thenReturn(true);
        when(tokenService.getTenantId()).thenReturn(null);

        Map<HelpTopic, AvailableHelpTopic> topics = service.availableForCurrentUser();

        assertThat(topics).containsKey(HelpTopic.ADMIN_USERS);
        assertThat(topics).doesNotContainKey(HelpTopic.ADMIN_ORGANIZATIONS);
    }

    @Test
    void tenantAdminGetsTenantIdOnTenantScopedTopics() {
        permissions(Map.of());
        when(userService.getOrganizationMembershipsForUser(5L)).thenReturn(List.of());
        when(securityService.isTenantAdmin()).thenReturn(true);
        when(tokenService.getTenantId()).thenReturn(7L);

        assertThat(service.availableForCurrentUser().get(HelpTopic.ADMIN_ORGANIZATIONS).tenantId())
                .isEqualTo(7L);
    }
}
