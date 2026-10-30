/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.controller;

import com.emberone.platform.web.list.options.ListFilterOptionsContext;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.platform.web.list.options.ListFilterOptionsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/list-filter-options")
@RequiredArgsConstructor
public class OrganizationListFilterOptionsController {

    private final ListFilterOptionsService listFilterOptionsService;

    /**
     * Filter options for organization-workspace lists. Repeat {@code include} for keys registered
     * for {@link com.emberone.platform.web.list.options.ListFilterOptionsScope#ORGANIZATION}.
     */
    @GetMapping
    @PreAuthorize("@securityService.hasUserReadPermission(#organizationId)")
    public ResponseEntity<ListFilterOptionsDto> getFilterOptions(
            @PathVariable long organizationId,
            @RequestParam(name = "include") List<String> include) {
        return ResponseEntity.ok(
                listFilterOptionsService.resolve(
                        ListFilterOptionsContext.organization(organizationId), include));
    }
}
