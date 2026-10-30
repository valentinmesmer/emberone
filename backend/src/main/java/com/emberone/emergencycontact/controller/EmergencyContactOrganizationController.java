/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.emergencycontact.controller;

import com.emberone.emergencycontact.dto.EmergencyContactDto;
import com.emberone.emergencycontact.service.EmergencyContactService;
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
@RequestMapping("/api/v1/auth/organizations/{organizationId}/emergency-contacts")
@RequiredArgsConstructor
public class EmergencyContactOrganizationController {

    private final EmergencyContactService emergencyContactService;

    @GetMapping
    @BoundedList("Per-user emergency contacts aggregated for org read; bounded per user.")
    @PreAuthorize("@securityService.hasEmergencyContactReadPermission(#organizationId)")
    public ResponseEntity<List<EmergencyContactDto>> getEmergencyContactsForOrganization(
            @PathVariable Long organizationId) {
        return ResponseEntity.ok(
                emergencyContactService.getEmergencyContactsForOrganization(organizationId));
    }
}
