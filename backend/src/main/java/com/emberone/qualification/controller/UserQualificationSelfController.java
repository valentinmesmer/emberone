/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.qualification.controller;

import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.qualification.dto.SelfQualificationDetailDto;
import com.emberone.qualification.dto.SelfQualificationStatusItemPageDto;
import com.emberone.qualification.list.QualificationListFilter;
import com.emberone.qualification.service.QualificationDetailService;
import com.emberone.qualification.service.QualificationStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/self/qualifications")
@RequiredArgsConstructor
public class UserQualificationSelfController {

    private final QualificationStatusService statusService;
    private final QualificationDetailService detailService;
    private final TokenService tokenService;

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SelfQualificationStatusItemPageDto> getStatus(
            ListQuery query, QualificationListFilter filter) {
        return ResponseEntity.ok(
                statusService.listSelfStatusItems(tokenService.requireUserId(), query, filter));
    }

    /**
     * One qualification with the evidence of its prerequisites; same access as the evidence APIs.
     */
    @GetMapping("/{definitionId}")
    @PreAuthorize("@securityService.isSelfServiceUser()")
    public ResponseEntity<SelfQualificationDetailDto> getDetail(@PathVariable Long definitionId) {
        return ResponseEntity.ok(
                detailService.getSelfDetail(tokenService.requireUserId(), definitionId));
    }
}
