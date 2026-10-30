/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.controller;

import com.emberone.organization.dto.OrganizationDto;
import com.emberone.organizationgroup.dto.OrganizationGroupDto;
import com.emberone.organizationgroup.dto.OrganizationGroupJoinTargetDto;
import com.emberone.organizationgroup.dto.OrganizationGroupLeaveTargetDto;
import com.emberone.organizationgroup.service.OrganizationGroupService;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.BoundedList;
import com.emberone.user.service.UserService;
import com.emberone.userorganizationmembership.dto.UserOrganizationMembershipDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/self/organizations")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class OrganizationSelfController {

    private final UserService userService;
    private final OrganizationGroupService organizationGroupService;
    private final TokenService tokenService;

    @GetMapping
    @BoundedList("Active organization memberships for the current user")
    public ResponseEntity<List<UserOrganizationMembershipDto>> getMyOrganizations() {
        List<UserOrganizationMembershipDto> memberships =
                userService.getOrganizationMembershipsForUser(tokenService.requireUserId());
        return ResponseEntity.ok(memberships);
    }

    @GetMapping("/joinable")
    @BoundedList("Workflow join targets for the current user")
    public ResponseEntity<List<OrganizationDto>> getJoinableOrganizations() {
        List<OrganizationDto> organizations =
                userService.getJoinableOrganizationsForUser(tokenService.requireUserId());
        return ResponseEntity.ok(organizations);
    }

    @GetMapping("/main-change-targets")
    @BoundedList("Eligible main-organization change targets for the current user")
    public ResponseEntity<List<OrganizationDto>> getMainOrganizationChangeTargets() {
        List<OrganizationDto> organizations =
                userService.getMainOrganizationChangeTargetsForUser(tokenService.requireUserId());
        return ResponseEntity.ok(organizations);
    }

    /**
     * Organizations the current user may request to leave. Orgs where the user is in the leadership
     * group are omitted; those memberships must be ended in tenant admin first.
     */
    @GetMapping("/leave-targets")
    @BoundedList("Eligible organization leave targets for the current user")
    public ResponseEntity<List<OrganizationDto>> getOrganizationLeaveTargets() {
        List<OrganizationDto> organizations =
                userService.getOrganizationLeaveTargetsForUser(tokenService.requireUserId());
        return ResponseEntity.ok(organizations);
    }

    /**
     * Groups the current user may request to leave. Leadership and all-members groups are omitted.
     */
    @GetMapping("/group-leave-targets")
    @BoundedList("Eligible group leave targets for the current user")
    public ResponseEntity<List<OrganizationGroupLeaveTargetDto>>
            getOrganizationGroupLeaveTargets() {
        List<OrganizationGroupLeaveTargetDto> groups =
                userService.getOrganizationGroupLeaveTargetsForUser(tokenService.requireUserId());
        return ResponseEntity.ok(groups);
    }

    /**
     * Groups the current user may request to join. Leadership, all-members, and groups the user
     * already belongs to are omitted.
     */
    @GetMapping("/group-join-targets")
    @BoundedList("Eligible group join targets for the current user")
    public ResponseEntity<List<OrganizationGroupJoinTargetDto>> getOrganizationGroupJoinTargets() {
        List<OrganizationGroupJoinTargetDto> groups =
                userService.getOrganizationGroupJoinTargetsForUser(tokenService.requireUserId());
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@securityService.isActiveOrganizationMember(#id)")
    public ResponseEntity<OrganizationDto> getMyOrganization(@PathVariable Long id) {
        OrganizationDto organization =
                userService.getOrganizationForMember(tokenService.requireUserId(), id);
        return ResponseEntity.ok(organization);
    }

    @GetMapping("/{organizationId}/groups")
    @BoundedList("Custom groups visible to the member in one organization")
    @PreAuthorize("@securityService.isActiveOrganizationMember(#organizationId)")
    public ResponseEntity<List<OrganizationGroupDto>> getMyOrganizationGroups(
            @PathVariable Long organizationId) {
        List<OrganizationGroupDto> groups =
                organizationGroupService.getGroupsForUserInOrganization(
                        tokenService.requireUserId(), organizationId, false);
        return ResponseEntity.ok(groups);
    }
}
