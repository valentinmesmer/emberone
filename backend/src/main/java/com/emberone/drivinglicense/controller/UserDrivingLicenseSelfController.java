/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.drivinglicense.controller;

import com.emberone.drivinglicense.dto.DrivingLicenseDefinitionDto;
import com.emberone.drivinglicense.dto.DrivingLicenseDto;
import com.emberone.drivinglicense.service.DrivingLicenseCatalogService;
import com.emberone.drivinglicense.service.DrivingLicenseRecordService;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.BoundedList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service driving license reads: own license and membership-org catalog definitions.
 * Submitting proofs uses {@link
 * com.emberone.workflow.drivinglicense.DrivingLicenseRecordWorkflowController}. HR management of
 * other users uses {@link UserDrivingLicenseOrganizationController}.
 */
@RestController
@RequestMapping("/api/v1/self/driving-license")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class UserDrivingLicenseSelfController {

    private final DrivingLicenseRecordService recordService;
    private final DrivingLicenseCatalogService catalogService;
    private final TokenService tokenService;

    @GetMapping
    public ResponseEntity<DrivingLicenseDto> getMyLicense() {
        return recordService
                .findLicense(tokenService.requireUserId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Catalog definitions for the caller's membership organization (for Nachweis einreichen). */
    @GetMapping("/available-definitions")
    @BoundedList("Org-standard driving license definition catalog")
    public ResponseEntity<List<DrivingLicenseDefinitionDto>> getMyAvailableDefinitions() {
        return ResponseEntity.ok(
                catalogService.listAvailableDefinitionsForUser(tokenService.requireUserId()));
    }
}
