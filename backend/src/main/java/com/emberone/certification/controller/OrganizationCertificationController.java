/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.controller;

import com.emberone.certification.dto.CertificationOverviewCountsDto;
import com.emberone.certification.dto.CertificationOverviewRowDto;
import com.emberone.certification.list.CertificationOrganizationListFilterOptions;
import com.emberone.certification.list.CertificationOverviewListFilter;
import com.emberone.certification.service.CertificationStatusService;
import com.emberone.platform.web.PageDto;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
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
@RequestMapping("/api/v1/auth/organizations/{organizationId}/certifications")
@RequiredArgsConstructor
public class OrganizationCertificationController {

    private final CertificationStatusService statusService;
    private final CertificationOrganizationListFilterOptions organizationListFilterOptions;

    @GetMapping("/status-overview/list-filter-options")
    @PreAuthorize("@securityService.hasCertificationReadPermission(#organizationId)")
    public ResponseEntity<ListFilterOptionsDto> listFilterOptions(
            @PathVariable Long organizationId) {
        return ResponseEntity.ok(organizationListFilterOptions.forOrganization(organizationId));
    }

    @GetMapping("/status-overview")
    @PreAuthorize("@securityService.hasCertificationReadPermission(#organizationId)")
    public ResponseEntity<PageDto<CertificationOverviewRowDto>> getStatusOverview(
            @PathVariable Long organizationId,
            ListQuery query,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<String> type) {
        return ResponseEntity.ok(
                statusService.getStatusOverview(
                        organizationId,
                        query,
                        CertificationOverviewListFilter.parse(status, type)));
    }

    @GetMapping("/status-overview/counts")
    @PreAuthorize("@securityService.hasCertificationReadPermission(#organizationId)")
    public ResponseEntity<CertificationOverviewCountsDto> getStatusOverviewCounts(
            @PathVariable Long organizationId) {
        return ResponseEntity.ok(statusService.getStatusOverviewCounts(organizationId));
    }
}
