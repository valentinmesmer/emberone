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
import com.emberone.absence.list.AbsenceOrganizationUserListFilterOptions;
import com.emberone.absence.service.UserAbsenceService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/users/{userId}/absences")
@RequiredArgsConstructor
public class UserAbsenceUserOrganizationController {

    private final UserAbsenceService userAbsenceService;
    private final UserService userService;
    private final AbsenceOrganizationUserListFilterOptions organizationUserListFilterOptions;

    @GetMapping("/list-filter-options")
    @PreAuthorize("@securityService.hasAbsenceReadPermission(#organizationId)")
    public ResponseEntity<ListFilterOptionsDto> listFilterOptions(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(
                organizationUserListFilterOptions.forUserInOrganization(userId, organizationId));
    }

    @GetMapping
    @PreAuthorize("@securityService.hasAbsenceReadPermission(#organizationId)")
    public ResponseEntity<UserAbsencePageDto> listAbsences(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            ListQuery query,
            @RequestParam(required = false) List<String> period,
            @RequestParam(required = false) List<String> reason,
            @RequestParam(name = "organizationId", required = false) List<Long> organizationIds,
            @RequestParam(required = false) List<Integer> year) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(
                userAbsenceService.listAbsencesForUserInOrganization(
                        userId,
                        organizationId,
                        query,
                        AbsenceListFilter.parse(period, reason, organizationIds, year)));
    }

    @PostMapping
    @PreAuthorize("@securityService.hasAbsenceWritePermission(#organizationId)")
    public ResponseEntity<UserAbsenceDto> createAbsence(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @Valid @RequestBody CreateUserAbsenceRequest request) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        UserAbsenceDto created =
                userAbsenceService.createAbsenceInOrganization(userId, organizationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{absenceId}")
    @PreAuthorize("@securityService.hasAbsenceWritePermission(#organizationId)")
    public ResponseEntity<UserAbsenceDto> updateAbsence(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable Long absenceId,
            @Valid @RequestBody UpdateUserAbsenceRequest request) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(
                userAbsenceService.updateAbsenceInOrganization(
                        userId, organizationId, absenceId, request));
    }

    @DeleteMapping("/{absenceId}")
    @PreAuthorize("@securityService.hasAbsenceWritePermission(#organizationId)")
    public ResponseEntity<Void> deleteAbsence(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable Long absenceId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        userAbsenceService.deleteAbsenceInOrganization(userId, organizationId, absenceId);
        return ResponseEntity.noContent().build();
    }
}
