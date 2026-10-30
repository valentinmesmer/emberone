/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.controller;

import com.emberone.absence.dto.CreateUserAbsenceRequest;
import com.emberone.absence.dto.UpdateUserAbsenceRequest;
import com.emberone.absence.dto.UserAbsenceDto;
import com.emberone.absence.dto.UserAbsencePageDto;
import com.emberone.absence.list.AbsenceListFilter;
import com.emberone.absence.list.AbsenceSelfListFilterOptions;
import com.emberone.absence.service.UserAbsenceService;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/self/absences")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class UserAbsenceSelfController {

    private final UserAbsenceService userAbsenceService;
    private final TokenService tokenService;
    private final AbsenceSelfListFilterOptions selfListFilterOptions;

    @GetMapping("/list-filter-options")
    public ResponseEntity<ListFilterOptionsDto> listFilterOptions() {
        return ResponseEntity.ok(selfListFilterOptions.filterOptions());
    }

    @GetMapping("/{absenceId}")
    public ResponseEntity<UserAbsenceDto> getAbsence(@PathVariable Long absenceId) {
        return ResponseEntity.ok(
                userAbsenceService.getAbsenceForUser(tokenService.requireUserId(), absenceId));
    }

    @GetMapping
    public ResponseEntity<UserAbsencePageDto> listAbsences(
            ListQuery query,
            @RequestParam(required = false) List<String> period,
            @RequestParam(required = false) List<String> reason,
            @RequestParam(required = false) List<Long> organizationId,
            @RequestParam(required = false) List<Integer> year) {
        return ResponseEntity.ok(
                userAbsenceService.listAbsencesForUser(
                        tokenService.requireUserId(),
                        query,
                        AbsenceListFilter.parse(period, reason, organizationId, year)));
    }

    @PostMapping
    public ResponseEntity<UserAbsenceDto> createAbsence(
            @Valid @RequestBody CreateUserAbsenceRequest request) {
        UserAbsenceDto created =
                userAbsenceService.createAbsence(tokenService.requireUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{absenceId}")
    public ResponseEntity<UserAbsenceDto> updateAbsence(
            @PathVariable Long absenceId, @Valid @RequestBody UpdateUserAbsenceRequest request) {
        return ResponseEntity.ok(
                userAbsenceService.updateAbsence(tokenService.requireUserId(), absenceId, request));
    }

    @DeleteMapping("/{absenceId}")
    public ResponseEntity<Void> deleteAbsence(@PathVariable Long absenceId) {
        userAbsenceService.deleteAbsence(tokenService.requireUserId(), absenceId);
        return ResponseEntity.noContent().build();
    }
}
