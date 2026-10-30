/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.user.controller;

import com.emberone.admin.user.dto.TenantUserListItemDto;
import com.emberone.admin.user.list.AdminUserListFilter;
import com.emberone.admin.user.list.AdminUserListFilterOptions;
import com.emberone.admin.user.service.AdminUserService;
import com.emberone.platform.web.PageDto;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AdminUserListFilterOptions adminUserListFilterOptions;

    @GetMapping("/list-filter-options")
    @PreAuthorize("@securityService.isAdminUser()")
    public ResponseEntity<ListFilterOptionsDto> listFilterOptions() {
        return ResponseEntity.ok(adminUserListFilterOptions.filterOptions());
    }

    @GetMapping
    @PreAuthorize("@securityService.isAdminUser()")
    public ResponseEntity<PageDto<TenantUserListItemDto>> listUsers(
            ListQuery query,
            @RequestParam(required = false) List<Long> tenantId,
            @RequestParam(required = false) List<Long> organizationId,
            @RequestParam(required = false) List<String> appRole) {
        return ResponseEntity.ok(
                adminUserService.listUsers(
                        query, AdminUserListFilter.parse(tenantId, organizationId, appRole)));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("@securityService.isAdminUser()")
    public ResponseEntity<TenantUserListItemDto> getUser(@PathVariable long userId) {
        return ResponseEntity.ok(adminUserService.getUser(userId));
    }
}
