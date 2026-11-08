/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai.helpchat;

import com.emberone.organization.dto.OrganizationDto;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.security.SecurityService;
import com.emberone.platform.security.TokenService;
import com.emberone.user.service.UserService;
import com.emberone.userorganizationmembership.dto.UserOrganizationMembershipDto;
import com.emberone.userpermission.service.UserPermissionService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Decides which {@link HelpTopic}s the current user may be pointed to, using the same sources as
 * the frontend navigation: the effective permission map ({@code GET /api/v1/self/permissions}) per
 * organization, the user's own memberships for display names, and the admin role checks of {@link
 * SecurityService}. Everything is loaded in the caller's context; nothing here widens access, the
 * target pages and APIs check permissions again.
 */
@Service
@RequiredArgsConstructor
public class HelpTopicAvailabilityService {

    private final TokenService tokenService;
    private final SecurityService securityService;
    private final UserPermissionService userPermissionService;
    private final UserService userService;
    private final HelpTextProvider helpTextProvider;

    public Map<HelpTopic, AvailableHelpTopic> availableForCurrentUser() {
        long userId = tokenService.requireUserId();
        Map<Long, List<PermissionType>> permissions =
                userPermissionService.getUserPermissions(userId).getPermissions();
        Map<Long, String> organizationNames = organizationNames(userId);
        boolean admin = securityService.isTenantAdmin() || securityService.isGlobalAdmin();
        Long tenantId = tokenService.getTenantId();

        Map<HelpTopic, AvailableHelpTopic> result = new EnumMap<>(HelpTopic.class);
        for (HelpTopic topic : HelpTopic.values()) {
            if (!helpTextProvider.hasTopic(topic.name())) {
                continue;
            }
            switch (topic.getArea()) {
                case SELF ->
                        result.put(
                                topic, new AvailableHelpTopic(topic, List.of(), List.of(), null));
                case ORGANIZATION -> {
                    List<HelpChatOrganizationDto> read =
                            organizationsWith(
                                    topic.getReadPermission(), permissions, organizationNames);
                    if (!read.isEmpty()) {
                        List<HelpChatOrganizationDto> create =
                                topic.getCreatePermission() == null
                                        ? List.of()
                                        : organizationsWith(
                                                topic.getCreatePermission(),
                                                permissions,
                                                organizationNames);
                        result.put(topic, new AvailableHelpTopic(topic, read, create, null));
                    }
                }
                case ADMIN -> {
                    if (admin && (!topic.isTenantScoped() || tenantId != null)) {
                        result.put(
                                topic,
                                new AvailableHelpTopic(
                                        topic,
                                        List.of(),
                                        List.of(),
                                        topic.isTenantScoped() ? tenantId : null));
                    }
                }
            }
        }
        return result;
    }

    private Map<Long, String> organizationNames(long userId) {
        Map<Long, String> names = new LinkedHashMap<>();
        for (UserOrganizationMembershipDto membership :
                userService.getOrganizationMembershipsForUser(userId)) {
            OrganizationDto organization = membership.getOrganization();
            if (organization != null && organization.getId() != null) {
                names.put(organization.getId(), organization.getName());
            }
        }
        return names;
    }

    /** Organizations with the permission that the user is a member of, sorted by name. */
    static List<HelpChatOrganizationDto> organizationsWith(
            PermissionType permission,
            Map<Long, List<PermissionType>> permissions,
            Map<Long, String> organizationNames) {
        if (permission == null || permissions == null) {
            return List.of();
        }
        List<HelpChatOrganizationDto> organizations = new ArrayList<>();
        permissions.forEach((organizationId, types) -> {
            String name = organizationNames.get(organizationId);
            if (types != null && types.contains(permission) && name != null) {
                organizations.add(new HelpChatOrganizationDto(organizationId, name));
            }
        });
        organizations.sort(
                Comparator.comparing(HelpChatOrganizationDto::name, String.CASE_INSENSITIVE_ORDER));
        return organizations;
    }
}
