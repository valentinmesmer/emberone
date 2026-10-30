/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.controller;

import com.emberone.dutyfunction.dto.ResolvedDutyFunctionDto;
import com.emberone.dutyfunction.service.DutyFunctionResolutionService;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.BoundedList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/self/functions")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class UserDutyFunctionSelfController {

    private final DutyFunctionResolutionService dutyFunctionResolutionService;
    private final TokenService tokenService;

    @GetMapping
    @BoundedList("Resolved duty functions for the current user")
    public ResponseEntity<List<ResolvedDutyFunctionDto>> getMyFunctions() {
        return ResponseEntity.ok(
                dutyFunctionResolutionService.resolveSelfByOrganization(
                        tokenService.requireUserId()));
    }
}
