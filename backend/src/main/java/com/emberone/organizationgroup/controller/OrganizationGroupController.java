/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.controller;

import com.emberone.organizationgroup.dto.OrganizationGroupDto;
import com.emberone.organizationgroup.dto.OrganizationGroupMemberPageDto;
import com.emberone.organizationgroup.dto.OrganizationGroupPageDto;
import com.emberone.organizationgroup.list.OrganizationGroupListFilter;
import com.emberone.organizationgroup.service.OrganizationGroupService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.userorganizationgroupmembership.service.UserOrganizationGroupMembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/organizations/groups")
@RequiredArgsConstructor
public class OrganizationGroupController {

    private final OrganizationGroupService organizationGroupService;
    private final UserOrganizationGroupMembershipService userOrganizationGroupMembershipService;

    @GetMapping
    @PreAuthorize(
            "@securityService.hasOrganizationGroupReadPermissionForOrganization(#organizationId)")
    public ResponseEntity<OrganizationGroupPageDto> getGroupsByOrganization(
            @RequestParam Long organizationId,
            ListQuery query,
            OrganizationGroupListFilter filter) {
        return ResponseEntity.ok(
                organizationGroupService.listVisibleGroupsByOrganizationId(
                        organizationId, query, filter));
    }

    @GetMapping("/{groupId}")
    @PreAuthorize("@securityService.hasOrganizationGroupReadPermission(#groupId)")
    public ResponseEntity<OrganizationGroupDto> getOrganizationGroupById(
            @PathVariable Long groupId) {
        OrganizationGroupDto group = organizationGroupService.getGroupById(groupId);
        return ResponseEntity.ok(group);
    }

    @GetMapping("/{groupId}/members")
    @PreAuthorize("@securityService.hasOrganizationGroupReadPermission(#groupId)")
    public ResponseEntity<OrganizationGroupMemberPageDto> getOrganizationGroupMembers(
            @PathVariable Long groupId, ListQuery query) {
        return ResponseEntity.ok(
                userOrganizationGroupMembershipService.pageMembersByOrganizationGroupId(
                        groupId, query));
    }
}
