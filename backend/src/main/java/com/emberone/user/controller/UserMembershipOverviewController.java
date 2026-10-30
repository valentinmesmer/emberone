/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.controller;

import com.emberone.organization.dto.OrganizationDto;
import com.emberone.platform.web.list.BoundedList;
import com.emberone.user.dto.OrganizationMembershipSectionDto;
import com.emberone.user.service.UserMembershipOverviewService;
import com.emberone.user.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/users/{userId}")
@RequiredArgsConstructor
public class UserMembershipOverviewController {

    private final UserMembershipOverviewService membershipOverviewService;
    private final UserService userService;

    @GetMapping("/membership-overview")
    @PreAuthorize("@securityService.hasUserReadPermission(#organizationId)")
    public ResponseEntity<OrganizationMembershipSectionDto> getMembershipOverview(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(
                membershipOverviewService.getMembershipOverview(organizationId, userId));
    }

    @GetMapping("/main-change-targets")
    @BoundedList("Eligible main-organization change targets for one member")
    @PreAuthorize("@securityService.hasUserReadPermission(#organizationId)")
    public ResponseEntity<List<OrganizationDto>> getMainOrganizationChangeTargets(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(
                userService.getMainOrganizationChangeTargetsForMember(organizationId, userId));
    }
}
