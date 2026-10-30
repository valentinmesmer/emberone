/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.controller;

import com.emberone.platform.http.ContentDispositionHeaders;
import com.emberone.platform.web.list.BoundedList;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.userdocument.dto.UpdateUserDocumentRequest;
import com.emberone.userdocument.dto.UserDocumentDto;
import com.emberone.userdocument.dto.UserDocumentPageDto;
import com.emberone.userdocument.list.UserDocumentListFilter;
import com.emberone.userdocument.list.UserDocumentListFilterOptions;
import com.emberone.userdocument.service.UserDocumentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/users/{userId}/documents")
@RequiredArgsConstructor
public class UserDocumentOrganizationController {

    private final UserDocumentService userDocumentService;
    private final UserDocumentListFilterOptions userDocumentListFilterOptions;

    @GetMapping("/list-filter-options")
    @PreAuthorize("@securityService.hasDocumentReadPermission(#organizationId)")
    public ResponseEntity<ListFilterOptionsDto> listFilterOptions(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        userDocumentService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(userDocumentListFilterOptions.forUser(userId));
    }

    @GetMapping
    @PreAuthorize("@securityService.hasDocumentReadPermission(#organizationId)")
    public ResponseEntity<UserDocumentPageDto> listDocuments(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            ListQuery query,
            UserDocumentListFilter filter) {
        userDocumentService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(userDocumentService.listDocuments(userId, query, filter));
    }

    @GetMapping("/tags")
    @PreAuthorize("@securityService.hasDocumentReadPermission(#organizationId)")
    @BoundedList("Distinct tags per user document archive")
    public ResponseEntity<List<String>> listTags(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        userDocumentService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(userDocumentService.listDocumentTags(userId));
    }

    @GetMapping("/{documentId}/download")
    @PreAuthorize("@securityService.hasDocumentReadPermission(#organizationId)")
    public ResponseEntity<InputStreamResource> downloadDocument(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable Long documentId) {
        userDocumentService.assertUserBelongsToOrganization(userId, organizationId);
        UserDocumentService.DocumentDownload download =
                userDocumentService.downloadDocument(userId, documentId);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDispositionHeaders.attachment(download.fileName()))
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.resource());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@securityService.hasDocumentWritePermission(#organizationId)")
    public ResponseEntity<UserDocumentDto> uploadDocument(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) List<String> tags) {
        userDocumentService.assertUserBelongsToOrganization(userId, organizationId);
        UserDocumentDto created =
                userDocumentService.uploadDocument(userId, file, title, description, tags);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{documentId}")
    @PreAuthorize("@securityService.hasDocumentWritePermission(#organizationId)")
    public ResponseEntity<UserDocumentDto> updateDocument(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable Long documentId,
            @Valid @RequestBody UpdateUserDocumentRequest request) {
        userDocumentService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(userDocumentService.updateDocument(userId, documentId, request));
    }
}
