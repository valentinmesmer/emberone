/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationgroupmembership.controller;

import com.emberone.userorganizationgroupmembership.dto.CreateUserOrganizationGroupMembershipRequest;
import com.emberone.userorganizationgroupmembership.dto.UpdateUserOrganizationGroupMembershipRequest;
import com.emberone.userorganizationgroupmembership.dto.UserOrganizationGroupMembershipDto;
import com.emberone.userorganizationgroupmembership.service.UserOrganizationGroupMembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/users/organization-group-memberships")
@RequiredArgsConstructor
public class UserOrganizationGroupMembershipController {

    private final UserOrganizationGroupMembershipService membershipService;

    @PostMapping
    @PreAuthorize(
            "@securityService.hasUserWritePermissionForOrganizationGroup(#request.organizationGroupId)")
    public ResponseEntity<UserOrganizationGroupMembershipDto> createMembership(
            @Valid @RequestBody CreateUserOrganizationGroupMembershipRequest request) {
        UserOrganizationGroupMembershipDto created = membershipService.createMembership(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{membershipId}")
    @PreAuthorize("@securityService.hasUserWritePermissionForMembership(#membershipId)")
    public ResponseEntity<UserOrganizationGroupMembershipDto> updateMembership(
            @PathVariable Long membershipId,
            @Valid @RequestBody UpdateUserOrganizationGroupMembershipRequest request) {
        UserOrganizationGroupMembershipDto updated =
                membershipService.updateMembership(membershipId, request);
        return ResponseEntity.ok(updated);
    }
}
