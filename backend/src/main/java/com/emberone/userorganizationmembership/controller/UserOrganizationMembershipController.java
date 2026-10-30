/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.controller;

import com.emberone.userorganizationmembership.dto.UpdateUserOrganizationMembershipRequest;
import com.emberone.userorganizationmembership.dto.UserOrganizationMembershipDto;
import com.emberone.userorganizationmembership.service.UserOrganizationMembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/users/organization-memberships")
@RequiredArgsConstructor
public class UserOrganizationMembershipController {

    private final UserOrganizationMembershipService membershipService;

    @PutMapping("/{membershipId}")
    @PreAuthorize("@securityService.hasUserWritePermissionForOrganizationMembership(#membershipId)")
    public ResponseEntity<UserOrganizationMembershipDto> updateMembership(
            @PathVariable Long membershipId,
            @Valid @RequestBody UpdateUserOrganizationMembershipRequest request) {
        UserOrganizationMembershipDto updated =
                membershipService.updateMembership(membershipId, request);
        return ResponseEntity.ok(updated);
    }
}
