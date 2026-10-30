/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.controller;

import com.emberone.onboardingdocument.service.SelfEntryDocumentProofExportService;
import com.emberone.platform.http.ContentDispositionHeaders;
import com.emberone.platform.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/self/entry-documents")
@RequiredArgsConstructor
@PreAuthorize("@securityService.isSelfServiceUser()")
public class SelfEntryDocumentFulfillmentController {

    private final SelfEntryDocumentProofExportService selfEntryDocumentProofExportService;
    private final TokenService tokenService;

    @GetMapping("/proof-export")
    public ResponseEntity<byte[]> exportProof() {
        long userId = tokenService.requireUserId();
        byte[] pdf = selfEntryDocumentProofExportService.exportProofPdf(userId);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDispositionHeaders.attachment("eintrittsdokumente-nachweis.pdf"))
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
