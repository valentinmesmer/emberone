/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.controller;

import com.emberone.platform.http.ContentDispositionHeaders;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.BoundedList;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.userdocument.dto.UnreadDocumentCountDto;
import com.emberone.userdocument.dto.UserDocumentPageDto;
import com.emberone.userdocument.list.UserDocumentListFilter;
import com.emberone.userdocument.list.UserDocumentSelfListFilterOptions;
import com.emberone.userdocument.service.UserDocumentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/self/documents")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class UserDocumentSelfController {

    private final UserDocumentService userDocumentService;
    private final TokenService tokenService;
    private final UserDocumentSelfListFilterOptions selfListFilterOptions;

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadDocumentCountDto> getUnreadCount() {
        return ResponseEntity.ok(
                new UnreadDocumentCountDto(
                        userDocumentService.getUnreadCount(tokenService.requireUserId())));
    }

    @PostMapping("/{documentId}/mark-viewed")
    public ResponseEntity<Void> markViewed(@PathVariable Long documentId) {
        userDocumentService.markViewed(tokenService.requireUserId(), documentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mark-all-viewed")
    public ResponseEntity<Void> markAllViewed() {
        userDocumentService.markAllViewed(tokenService.requireUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/list-filter-options")
    public ResponseEntity<ListFilterOptionsDto> listFilterOptions() {
        return ResponseEntity.ok(selfListFilterOptions.filterOptions());
    }

    @GetMapping
    public ResponseEntity<UserDocumentPageDto> listDocuments(
            ListQuery query, UserDocumentListFilter filter) {
        return ResponseEntity.ok(
                userDocumentService.listDocuments(tokenService.requireUserId(), query, filter));
    }

    @GetMapping("/tags")
    @BoundedList("Distinct tags per user document archive")
    public ResponseEntity<List<String>> listTags() {
        return ResponseEntity.ok(
                userDocumentService.listDocumentTags(tokenService.requireUserId()));
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<InputStreamResource> downloadDocument(@PathVariable Long documentId) {
        UserDocumentService.DocumentDownload download =
                userDocumentService.downloadDocument(tokenService.requireUserId(), documentId);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDispositionHeaders.attachment(download.fileName()))
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.resource());
    }
}
