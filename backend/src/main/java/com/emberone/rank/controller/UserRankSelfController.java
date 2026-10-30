/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.controller;

import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.BoundedList;
import com.emberone.rank.dto.TenantRankDto;
import com.emberone.rank.dto.UserRankSelfDto;
import com.emberone.rank.service.TenantRankCatalogService;
import com.emberone.rank.service.UserRankService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/self/rank")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class UserRankSelfController {

    private final UserRankService userRankService;
    private final TenantRankCatalogService tenantRankCatalogService;
    private final TokenService tokenService;

    @GetMapping
    public ResponseEntity<UserRankSelfDto> getMyRank() {
        return ResponseEntity.ok(userRankService.getSelfRank(tokenService.requireUserId()));
    }

    @GetMapping("/available-ranks")
    @BoundedList("Tenant rank catalog for assignment UI")
    public ResponseEntity<List<TenantRankDto>> listAvailableRanks() {
        long tenantId = tokenService.requireTenantId();
        return ResponseEntity.ok(tenantRankCatalogService.listActiveRanks(tenantId));
    }
}
