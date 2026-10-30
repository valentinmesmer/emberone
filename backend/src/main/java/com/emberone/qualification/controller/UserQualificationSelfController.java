/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.controller;

import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.qualification.dto.SelfQualificationStatusItemPageDto;
import com.emberone.qualification.list.QualificationListFilter;
import com.emberone.qualification.service.QualificationStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/self/qualifications")
@RequiredArgsConstructor
public class UserQualificationSelfController {

    private final QualificationStatusService statusService;
    private final TokenService tokenService;

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SelfQualificationStatusItemPageDto> getStatus(
            ListQuery query, QualificationListFilter filter) {
        return ResponseEntity.ok(
                statusService.listSelfStatusItems(tokenService.requireUserId(), query, filter));
    }
}
