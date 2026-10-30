/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.controller;

import com.emberone.certification.dto.CertificationDefinitionDto;
import com.emberone.certification.dto.UserCertificationRecordDto;
import com.emberone.certification.dto.UserCertificationStatusDto;
import com.emberone.certification.service.CertificationAssignmentService;
import com.emberone.certification.service.CertificationRecordService;
import com.emberone.certification.service.CertificationStatusService;
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
 * Self-service certification reads: own status, records, and unassigned catalog definitions (for
 * submitting proofs without a prior assignment). Submitting proofs uses {@link
 * com.emberone.workflow.certificationrecord.CertificationRecordWorkflowController}. HR management
 * of other users uses {@link UserCertificationOrganizationController}.
 */
@RestController
@RequestMapping("/api/v1/self/certifications")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class UserCertificationSelfController {

    private final CertificationStatusService statusService;
    private final CertificationRecordService recordService;
    private final CertificationAssignmentService assignmentService;
    private final TokenService tokenService;

    @GetMapping("/status")
    public ResponseEntity<UserCertificationStatusDto> getMyStatus() {
        return ResponseEntity.ok(statusService.getStatusForUser(tokenService.requireUserId()));
    }

    @GetMapping("/records")
    @BoundedList("Per-user certification records; cardinality bounded by assignments")
    public ResponseEntity<List<UserCertificationRecordDto>> getMyRecords() {
        return ResponseEntity.ok(recordService.listRecords(tokenService.requireUserId()));
    }

    /** Catalog definitions available for the caller's membership org that are not yet assigned. */
    @GetMapping("/requestable-definitions")
    @BoundedList("Tenant certification catalog filtered to unassigned definitions for caller")
    public ResponseEntity<List<CertificationDefinitionDto>> getMyRequestableDefinitions() {
        return ResponseEntity.ok(
                assignmentService.listRequestableDefinitionsForUser(tokenService.requireUserId()));
    }
}
