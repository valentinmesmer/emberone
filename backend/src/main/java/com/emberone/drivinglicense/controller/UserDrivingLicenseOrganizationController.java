/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.drivinglicense.controller;

import com.emberone.drivinglicense.dto.*;
import com.emberone.drivinglicense.service.DrivingLicenseCatalogService;
import com.emberone.drivinglicense.service.DrivingLicenseRecordService;
import com.emberone.platform.web.list.BoundedList;
import com.emberone.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/users/{userId}/driving-license")
@RequiredArgsConstructor
public class UserDrivingLicenseOrganizationController {

    private final DrivingLicenseRecordService recordService;
    private final DrivingLicenseCatalogService catalogService;
    private final UserService userService;

    @GetMapping
    @PreAuthorize("@securityService.hasDrivingLicenseReadPermission(#organizationId)")
    public ResponseEntity<DrivingLicenseDto> getLicense(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return recordService
                .findLicense(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/pending-workflow")
    @PreAuthorize("@securityService.hasDrivingLicenseReadPermission(#organizationId)")
    public ResponseEntity<DrivingLicensePendingWorkflowDto> getPendingWorkflow(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return recordService
                .findPendingWorkflow(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    @PreAuthorize("@securityService.hasDrivingLicenseWritePermission(#organizationId)")
    public ResponseEntity<DrivingLicenseDto> createLicense(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @Valid @RequestBody CreateDrivingLicenseRequest request) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recordService.createManual(organizationId, userId, request));
    }

    @PutMapping
    @PreAuthorize("@securityService.hasDrivingLicenseWritePermission(#organizationId)")
    public ResponseEntity<DrivingLicenseDto> updateLicense(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateDrivingLicenseRequest request) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(recordService.updateManual(organizationId, userId, request));
    }

    @DeleteMapping
    @PreAuthorize("@securityService.hasDrivingLicenseWritePermission(#organizationId)")
    public ResponseEntity<Void> deleteLicense(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        recordService.deleteManual(organizationId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/available-definitions")
    @BoundedList("Org-standard driving license definition catalog")
    @PreAuthorize("@securityService.hasDrivingLicenseReadPermission(#organizationId)")
    public ResponseEntity<OrgDrivingLicenseCatalogDto> getAvailableDefinitions(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        return ResponseEntity.ok(catalogService.getAvailableDefinitionsForOrg(organizationId));
    }
}
