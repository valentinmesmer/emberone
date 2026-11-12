/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.qualification.controller;

import com.emberone.platform.security.SecurityService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.qualification.dto.CreateQualificationOrgDecisionRequest;
import com.emberone.qualification.dto.QualificationStatusItemPageDto;
import com.emberone.qualification.dto.UserQualificationDetailDto;
import com.emberone.qualification.dto.UserQualificationOrgDecisionDto;
import com.emberone.qualification.list.QualificationListFilter;
import com.emberone.qualification.service.QualificationDetailService;
import com.emberone.qualification.service.QualificationOrgDecisionService;
import com.emberone.qualification.service.QualificationStatusService;
import com.emberone.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/users/{userId}/qualifications")
@RequiredArgsConstructor
public class UserQualificationOrganizationController {

    private final QualificationStatusService statusService;
    private final QualificationOrgDecisionService orgDecisionService;
    private final QualificationDetailService detailService;
    private final SecurityService securityService;
    private final UserService userService;

    @GetMapping("/status")
    @PreAuthorize("@securityService.hasQualificationReadPermission(#organizationId)")
    public ResponseEntity<QualificationStatusItemPageDto> getStatus(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            ListQuery query,
            QualificationListFilter filter) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(
                statusService.listStatusItemsForUser(organizationId, userId, query, filter));
    }

    /**
     * One qualification with the evidence of its prerequisites. Qualification read is required;
     * certification, training course and driving license sections are filled only with the matching
     * read permission.
     */
    @GetMapping("/{definitionId}")
    @PreAuthorize("@securityService.hasQualificationReadPermission(#organizationId)")
    public ResponseEntity<UserQualificationDetailDto> getDetail(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable Long definitionId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(
                detailService.getOrgDetail(
                        organizationId,
                        userId,
                        definitionId,
                        new QualificationDetailService.EvidenceAccess(
                                securityService.hasCertificationReadPermission(organizationId),
                                securityService.hasTrainingCourseReadPermission(organizationId),
                                securityService.hasDrivingLicenseReadPermission(organizationId))));
    }

    @PostMapping("/{qualificationConfigKey}/org-decision")
    @PreAuthorize("@securityService.hasQualificationWritePermission(#organizationId)")
    public ResponseEntity<UserQualificationOrgDecisionDto> grant(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable String qualificationConfigKey,
            @Valid @RequestBody CreateQualificationOrgDecisionRequest request) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        orgDecisionService.grant(
                                organizationId, userId, qualificationConfigKey, request));
    }

    @PutMapping("/{qualificationConfigKey}/org-decision")
    @PreAuthorize("@securityService.hasQualificationWritePermission(#organizationId)")
    public ResponseEntity<UserQualificationOrgDecisionDto> updateGrant(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable String qualificationConfigKey,
            @Valid @RequestBody CreateQualificationOrgDecisionRequest request) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(
                orgDecisionService.updateGrant(
                        organizationId, userId, qualificationConfigKey, request));
    }
}
