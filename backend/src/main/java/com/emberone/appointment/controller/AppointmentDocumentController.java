/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.controller;

import com.emberone.appointment.service.AppointmentDocumentService;
import com.emberone.attachment.dto.EntityAttachmentDto;
import com.emberone.platform.http.ContentDispositionHeaders;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.BoundedList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(
        "/api/v1/auth/organizations/{organizationId}/appointments/{appointmentId}/attachments")
@RequiredArgsConstructor
public class AppointmentDocumentController {

    private final AppointmentDocumentService appointmentDocumentService;
    private final TokenService tokenService;

    @GetMapping
    @BoundedList("Attachments per appointment are naturally bounded")
    @PreAuthorize("@securityService.hasAppointmentReadPermission(#organizationId)")
    public ResponseEntity<List<EntityAttachmentDto>> listAttachments(
            @PathVariable Long organizationId, @PathVariable Long appointmentId) {
        return ResponseEntity.ok(
                appointmentDocumentService.listAttachments(organizationId, appointmentId));
    }

    @PostMapping
    @PreAuthorize("@securityService.hasAppointmentWritePermission(#organizationId)")
    public ResponseEntity<EntityAttachmentDto> uploadAttachment(
            @PathVariable Long organizationId,
            @PathVariable Long appointmentId,
            @RequestParam("file") MultipartFile file) {
        Long userId = requireUserId();
        EntityAttachmentDto created =
                appointmentDocumentService.uploadAttachment(
                        userId, organizationId, appointmentId, file);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{attachmentId}/download")
    @PreAuthorize("@securityService.hasAppointmentReadPermission(#organizationId)")
    public ResponseEntity<InputStreamResource> downloadAttachment(
            @PathVariable Long organizationId,
            @PathVariable Long appointmentId,
            @PathVariable Long attachmentId) {
        InputStreamResource resource =
                appointmentDocumentService.downloadAttachment(
                        organizationId, appointmentId, attachmentId);
        String fileName =
                appointmentDocumentService.getFileName(organizationId, appointmentId, attachmentId);
        String contentType =
                appointmentDocumentService.getContentType(
                        organizationId, appointmentId, attachmentId);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDispositionHeaders.attachment(fileName))
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("@securityService.hasAppointmentWritePermission(#organizationId)")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable Long organizationId,
            @PathVariable Long appointmentId,
            @PathVariable Long attachmentId) {
        appointmentDocumentService.deleteAttachment(organizationId, appointmentId, attachmentId);
        return ResponseEntity.noContent().build();
    }

    private Long requireUserId() {
        Long userId = tokenService.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID is required");
        }
        return userId;
    }
}
