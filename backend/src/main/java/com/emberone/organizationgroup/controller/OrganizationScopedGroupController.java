/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organizationgroup.controller;

import com.emberone.organizationgroup.dto.OrganizationGroupDto;
import com.emberone.organizationgroup.service.OrganizationGroupService;
import com.emberone.platform.web.list.BoundedList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/groups")
@RequiredArgsConstructor
public class OrganizationScopedGroupController {

    private final OrganizationGroupService organizationGroupService;

    @GetMapping("/assignable")
    @BoundedList("Assignable groups for user write UI in one organization")
    @PreAuthorize("@securityService.hasUserWritePermission(#organizationId)")
    public ResponseEntity<List<OrganizationGroupDto>> getAssignableGroups(
            @PathVariable Long organizationId) {
        return ResponseEntity.ok(
                organizationGroupService.getAssignableGroupsByOrganizationId(organizationId));
    }

    @GetMapping("/audience")
    @BoundedList("Audience picker groups for appointments in one organization")
    @PreAuthorize("@securityService.hasAppointmentWritePermission(#organizationId)")
    public ResponseEntity<List<OrganizationGroupDto>> getAudienceGroups(
            @PathVariable Long organizationId) {
        return ResponseEntity.ok(
                organizationGroupService.getAudienceGroupsByOrganizationId(organizationId));
    }
}
