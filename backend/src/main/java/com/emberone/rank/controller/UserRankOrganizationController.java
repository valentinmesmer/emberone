/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.controller;

import com.emberone.dutyfunction.dto.ResolvedDutyFunctionDto;
import com.emberone.dutyfunction.service.DutyFunctionResolutionService;
import com.emberone.platform.web.list.BoundedList;
import com.emberone.rank.dto.SetUserRankRequest;
import com.emberone.rank.dto.UserRankCurrentDto;
import com.emberone.rank.dto.UserRankSelfDto;
import com.emberone.rank.service.UserRankService;
import com.emberone.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/users/{userId}")
@RequiredArgsConstructor
public class UserRankOrganizationController {

    private final UserRankService userRankService;
    private final DutyFunctionResolutionService dutyFunctionResolutionService;
    private final UserService userService;

    @GetMapping("/rank")
    @PreAuthorize("@securityService.hasRankReadPermission(#organizationId)")
    public ResponseEntity<UserRankSelfDto> getUserRank(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(userRankService.getSelfRank(userId));
    }

    @PostMapping("/rank")
    @PreAuthorize("@securityService.hasRankWritePermission(#organizationId)")
    public ResponseEntity<UserRankCurrentDto> setUserRank(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @Valid @RequestBody SetUserRankRequest request) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(
                userRankService.assignFromOrganization(organizationId, userId, request));
    }

    @GetMapping("/functions")
    @BoundedList("Resolved duty functions for one organization user")
    @PreAuthorize("@securityService.hasRankReadPermission(#organizationId)")
    public ResponseEntity<List<ResolvedDutyFunctionDto>> getUserFunctions(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(
                dutyFunctionResolutionService.resolveForUserInOrganization(userId, organizationId));
    }
}
