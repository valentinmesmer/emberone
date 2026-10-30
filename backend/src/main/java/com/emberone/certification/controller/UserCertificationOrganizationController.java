/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.controller;

import com.emberone.certification.dto.*;
import com.emberone.certification.service.CertificationAssignmentService;
import com.emberone.certification.service.CertificationCatalogService;
import com.emberone.certification.service.CertificationRecordService;
import com.emberone.certification.service.CertificationStatusService;
import com.emberone.platform.web.list.BoundedList;
import com.emberone.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin/manager certification API scoped to an organization. The path {@code organizationId} is the
 * permission context; for assignments it also serves as the default responsible organization.
 */
@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/users/{userId}/certifications")
@RequiredArgsConstructor
public class UserCertificationOrganizationController {

    private final CertificationStatusService statusService;
    private final CertificationAssignmentService assignmentService;
    private final CertificationRecordService recordService;
    private final CertificationCatalogService catalogService;
    private final UserService userService;

    @GetMapping("/status")
    @PreAuthorize("@securityService.hasCertificationReadPermission(#organizationId)")
    public ResponseEntity<UserCertificationStatusDto> getStatus(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(statusService.getStatusForUser(userId));
    }

    @GetMapping("/assignments")
    @BoundedList("Per-user certification assignments")
    @PreAuthorize("@securityService.hasCertificationReadPermission(#organizationId)")
    public ResponseEntity<List<UserCertificationAssignmentDto>> listAssignments(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(assignmentService.listAssignments(userId));
    }

    @PostMapping("/assignments")
    @PreAuthorize("@securityService.hasCertificationWritePermission(#organizationId)")
    public ResponseEntity<UserCertificationAssignmentDto> createAssignment(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @Valid @RequestBody CreateUserCertificationAssignmentRequest request) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        if (request.getResponsibleOrganizationId() == null) {
            request.setResponsibleOrganizationId(organizationId);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.createAssignment(organizationId, userId, request));
    }

    @DeleteMapping("/assignments/{assignmentId}")
    @PreAuthorize("@securityService.hasCertificationWritePermission(#organizationId)")
    public ResponseEntity<Void> deleteAssignment(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable Long assignmentId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        assignmentService.deleteAssignment(organizationId, userId, assignmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/records")
    @BoundedList("Per-user certification records")
    @PreAuthorize("@securityService.hasCertificationReadPermission(#organizationId)")
    public ResponseEntity<List<UserCertificationRecordDto>> listRecords(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(recordService.listRecords(userId));
    }

    @PostMapping("/records")
    @PreAuthorize("@securityService.hasCertificationWritePermission(#organizationId)")
    public ResponseEntity<UserCertificationRecordDto> createRecord(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @Valid @RequestBody CreateUserCertificationRecordRequest request) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recordService.createManual(userId, request));
    }

    @GetMapping("/available")
    @PreAuthorize("@securityService.hasCertificationReadPermission(#organizationId)")
    public ResponseEntity<OrgCertificationCatalogDto> getAvailableCatalog(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        return ResponseEntity.ok(catalogService.getCatalog(organizationId));
    }
}
