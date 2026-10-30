/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.controller;

import com.emberone.qualification.dto.OrganizationQualificationStatusOverviewDto;
import com.emberone.qualification.service.QualificationStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/qualifications")
@RequiredArgsConstructor
public class OrganizationQualificationController {

    private final QualificationStatusService statusService;

    @GetMapping("/status-overview")
    @PreAuthorize("@securityService.hasQualificationReadPermission(#organizationId)")
    public ResponseEntity<OrganizationQualificationStatusOverviewDto> getStatusOverview(
            @PathVariable Long organizationId, @RequestParam(required = false) String search) {
        return ResponseEntity.ok(statusService.getStatusOverview(organizationId, search));
    }
}
