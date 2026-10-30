/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.timeentry.controller;

import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.timeentry.dto.CreateUserTimeEntryRequest;
import com.emberone.timeentry.dto.UpdateUserTimeEntryRequest;
import com.emberone.timeentry.dto.UserTimeEntryDto;
import com.emberone.timeentry.dto.UserTimeEntryPageDto;
import com.emberone.timeentry.list.TimeEntryListFilter;
import com.emberone.timeentry.list.TimeEntrySelfListFilterOptions;
import com.emberone.timeentry.service.UserTimeEntryService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/self/time-entries")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class UserTimeEntrySelfController {

    private final UserTimeEntryService userTimeEntryService;
    private final TokenService tokenService;
    private final TimeEntrySelfListFilterOptions selfListFilterOptions;

    @GetMapping("/list-filter-options")
    public ResponseEntity<ListFilterOptionsDto> listFilterOptions() {
        return ResponseEntity.ok(selfListFilterOptions.filterOptions());
    }

    @GetMapping("/{entryId}")
    public ResponseEntity<UserTimeEntryDto> getTimeEntry(@PathVariable Long entryId) {
        return ResponseEntity.ok(
                userTimeEntryService.getTimeEntryForUser(tokenService.requireUserId(), entryId));
    }

    @GetMapping
    public ResponseEntity<UserTimeEntryPageDto> listTimeEntries(
            ListQuery query,
            @RequestParam(required = false) List<Long> organizationId,
            @RequestParam(required = false) List<Integer> year) {
        return ResponseEntity.ok(
                userTimeEntryService.listTimeEntriesForUser(
                        tokenService.requireUserId(),
                        query,
                        TimeEntryListFilter.forSelf(organizationId, year)));
    }

    @PostMapping
    public ResponseEntity<UserTimeEntryDto> createTimeEntry(
            @Valid @RequestBody CreateUserTimeEntryRequest request) {
        UserTimeEntryDto created =
                userTimeEntryService.createTimeEntry(tokenService.requireUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{entryId}")
    public ResponseEntity<UserTimeEntryDto> updateTimeEntry(
            @PathVariable Long entryId, @Valid @RequestBody UpdateUserTimeEntryRequest request) {
        return ResponseEntity.ok(
                userTimeEntryService.updateTimeEntry(
                        tokenService.requireUserId(), entryId, request));
    }

    @DeleteMapping("/{entryId}")
    public ResponseEntity<Void> deleteTimeEntry(@PathVariable Long entryId) {
        userTimeEntryService.deleteTimeEntry(tokenService.requireUserId(), entryId);
        return ResponseEntity.noContent().build();
    }
}
