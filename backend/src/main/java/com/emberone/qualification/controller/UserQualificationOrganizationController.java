/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.controller;

import com.emberone.platform.web.list.ListQuery;
import com.emberone.qualification.dto.CreateQualificationOrgDecisionRequest;
import com.emberone.qualification.dto.QualificationStatusItemPageDto;
import com.emberone.qualification.dto.UserQualificationOrgDecisionDto;
import com.emberone.qualification.list.QualificationListFilter;
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
